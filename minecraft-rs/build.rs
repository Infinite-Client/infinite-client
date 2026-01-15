use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::collections::{BTreeMap, BTreeSet, HashSet};
use std::process::Command;
use std::sync::atomic::{AtomicUsize, Ordering};
use serde::Deserialize;
use quote::{quote, format_ident};
use walkdir::WalkDir;
use rayon::prelude::*;

#[derive(Deserialize)]
struct MinecraftMetadata {
    #[serde(rename = "className")]
    class_name: String,
    methods: Vec<MethodMetadata>,
}

#[derive(Deserialize)]
struct MethodMetadata {
    name: String,
    descriptor: String,
    #[serde(rename = "isStatic")]
    is_static: bool,
}

// 予約語と識別子の安全化
fn safe_ident(name: &str) -> String {
    let s = name.replace('$', "") // $ を除去。CamelCaseになることを想定
        .replace('<', "init")
        .replace('>', "");
    match s.as_str() {
        "type" | "match" | "move" | "loop" | "yield" | "ref" | "fn" | "where" | "mod" 
        | "self" | "super" | "const" | "crate" | "pub" | "in" | "box" | "become" 
        | "virtual" | "override" | "priv" | "async" | "await" | "dyn" | "abstract" 
        | "true" | "false" | "as" => format!("{}_", s),
        _ => s,
    }
}

// スネークケース変換
fn to_snake_case(s: &str) -> String {
    let mut snake = String::new();
    for (i, ch) in s.chars().enumerate() {
        if i > 0 && ch.is_uppercase() { snake.push('_'); }
        snake.push(ch.to_ascii_lowercase());
    }
    snake.replace('$', "_").replace("__", "_")
}

fn get_method_suffix(desc: &str) -> String {
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};
    let mut hasher = DefaultHasher::new();
    desc.hash(&mut hasher);
    format!("{:x}", hasher.finish())
}

fn map_java_type_to_rust(chars: &mut std::iter::Peekable<std::str::Chars>) -> proc_macro2::TokenStream {
    match chars.next().expect("Unexpected end of descriptor") {
        'Z' => quote! { bool },
        'B' => quote! { i8 },
        'C' => quote! { u16 },
        'S' => quote! { i16 },
        'I' => quote! { i32 },
        'J' => quote! { i64 },
        'F' => quote! { f32 },
        'D' => quote! { f64 },
        'V' => quote! { () },
        'L' => {
            while let Some(c) = chars.next() { if c == ';' { break; } }
            quote! { *mut std::ffi::c_void }
        },
        '[' => {
            while let Some(&'[') = chars.peek() { chars.next(); }
            let _inner = map_java_type_to_rust(chars);
            quote! { *mut std::ffi::c_void }
        },
        _ => quote! { *mut std::ffi::c_void },
    }
}

fn parse_descriptor(desc: &str) -> (Vec<proc_macro2::TokenStream>, proc_macro2::TokenStream) {
    let mut params = Vec::new();
    let mut chars = desc.chars().peekable();
    if chars.next() == Some('(') {
        while let Some(&c) = chars.peek() {
            if c == ')' { chars.next(); break; }
            params.push(map_java_type_to_rust(&mut chars));
        }
    }
    let ret = map_java_type_to_rust(&mut chars);
    (params, ret)
}

fn main() {
    let json_dir = Path::new("../build/mappings/net/minecraft");
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
    
    if out_dir.exists() { let _ = fs::remove_dir_all(&out_dir); }
    fs::create_dir_all(&out_dir).unwrap();

    let entries: Vec<_> = WalkDir::new(json_dir)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().and_then(|s| s.to_str()) == Some("json"))
        .collect();

    let total_classes = entries.len();
    println!("cargo:warning=Step 1: Parsing {} JSON files...", total_classes);

    let class_data: Vec<_> = entries.iter().map(|entry| {
        let content = fs::read_to_string(entry.path()).unwrap();
        let metadata: MinecraftMetadata = serde_json::from_str(&content).unwrap();
        let parts: Vec<String> = metadata.class_name.split('.').map(|s| s.to_string()).collect();
        (metadata, parts)
    }).collect();

    let mut package_tree: BTreeMap<String, BTreeSet<String>> = BTreeMap::new();
    for (_, parts) in &class_data {
        for i in 0..parts.len()-1 {
            let parent = if i == 0 { "root".to_string() } else { parts[..i].join(".") };
            package_tree.entry(parent).or_default().insert(parts[i].clone());
        }
        let package_name = parts[..parts.len()-1].join(".");
        package_tree.entry(package_name).or_default().insert(parts.last().unwrap().clone());
    }

    let counter = AtomicUsize::new(0);
    println!("cargo:warning=Step 2: Generating class files...");

    class_data.par_iter().for_each(|(metadata, parts)| {
        let current = counter.fetch_add(1, Ordering::Relaxed);
        if current % 1000 == 0 { println!("cargo:warning=  Progress: {}/{}", current, total_classes); }

        let package_parts: Vec<String> = parts[..parts.len()-1].iter().map(|s| to_snake_case(s)).collect();
        let package_path = package_parts.join("/");
        let class_simple_name = parts.last().unwrap();
        
        let class_ident = format_ident!("{}", safe_ident(class_simple_name));
        let vtable_ident = format_ident!("{}VTable", safe_ident(class_simple_name));
        
        let mut vtable_fields = Vec::new();
        let mut method_impls = quote! {};
        let mut seen_methods = HashSet::new();

        for method in &metadata.methods {
            let suffix = get_method_suffix(&method.descriptor);
            let field_name_str = format!("{}_{}", to_snake_case(&method.name), suffix);
            
            // 重複チェック
            if !seen_methods.insert(field_name_str.clone()) { continue; }

            let field_ident = format_ident!("{}", field_name_str);
            let m_ident = format_ident!("{}_{}", safe_ident(&method.name), suffix);
            
            vtable_fields.push(quote! { pub #field_ident: usize });

            let (param_types, ret_type) = parse_descriptor(&method.descriptor);
            let arg_names: Vec<proc_macro2::Ident> = (0..param_types.len()).map(|i| format_ident!("arg{}", i)).collect();
            let mut c_param_types = Vec::new();
            if !method.is_static { c_param_types.push(quote! { *mut std::ffi::c_void }); }
            c_param_types.extend(param_types.clone());
            let mut call_args = Vec::new();
            if !method.is_static { call_args.push(quote! { self.ptr }); }
            for name in &arg_names { call_args.push(quote! { #name }); }

            method_impls.extend(quote! {
                pub fn #m_ident(&self, #(#arg_names: #param_types),*) -> #ret_type {
                    unsafe {
                        let table = #vtable_ident::get();
                        let func: unsafe extern "C" fn(#(#c_param_types),*) -> #ret_type = std::mem::transmute(table.#field_ident);
                        func(#(#call_args),*)
                    }
                }
            });
        }

        let class_code = quote! {
            #![allow(unused_imports, non_camel_case_types, dead_code)]
            use std::sync::OnceLock;
            #[repr(C)] pub struct #vtable_ident { #(#vtable_fields,)* }
            pub static VTABLE: OnceLock<#vtable_ident> = OnceLock::new();
            impl #vtable_ident {
                pub unsafe fn get() -> &'static Self { VTABLE.get().expect(concat!("VTable not initialized for ", stringify!(#class_ident))) }
            }
            pub struct #class_ident { pub ptr: *mut std::ffi::c_void }
            impl #class_ident {
                pub fn from_raw(ptr: *mut std::ffi::c_void) -> Self { Self { ptr } }
                #method_impls
            }
        };

        let target_dir = out_dir.join(&package_path);
        fs::create_dir_all(&target_dir).unwrap();
        let file_path = target_dir.join(format!("{}.rs", to_snake_case(class_simple_name)));
        fs::write(&file_path, class_code.to_string()).unwrap();
    });

    println!("cargo:warning=Step 3: Generating module structure...");
    package_tree.iter().for_each(|(parent, children)| {
        let content = children.iter().map(|c| {
            let snake = to_snake_case(c);
            let java_ident = format_ident!("{}", safe_ident(c));
            let mod_ident = format_ident!("{}", snake);
            
            // 警告抑制付きの re-export
            if snake == safe_ident(c) {
                quote! { pub mod #mod_ident; }
            } else {
                quote! {
                    pub mod #mod_ident;
                    #[allow(unused_imports)]
                    pub use self::#mod_ident as #java_ident;
                }
            }
        });
        
        let file_path = if parent == "root" { out_dir.join("bindings.rs") } else {
            let path_parts: Vec<String> = parent.split('.').map(to_snake_case).collect();
            out_dir.join(format!("{}.rs", path_parts.join("/")))
        };

        if let Some(p) = file_path.parent() { fs::create_dir_all(p).unwrap(); }
        let header = quote! { #![allow(unused_imports, non_camel_case_types, dead_code)] };
        fs::write(&file_path, format!("{}\n{}", header.to_string(), quote! { #(#content)* }.to_string())).unwrap();
    });

    println!("cargo:warning=Final Step: Rustfmt...");
    let _ = Command::new("rustfmt").arg("--edition").arg("2021").arg(out_dir.join("bindings.rs")).status();
}
