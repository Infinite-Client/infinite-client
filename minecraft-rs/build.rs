use quote::{format_ident, quote};
use serde::Deserialize;
use std::collections::{BTreeMap, BTreeSet, HashSet};
use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;
use walkdir::WalkDir;

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

// --- ユーティリティ ---

fn safe_ident(name: &str) -> String {
    let s = name.replace('$', "_").replace('<', "init").replace('>', "");
    match s.as_str() {
        "type" | "match" | "move" | "loop" | "yield" | "ref" | "fn" | "where" | "mod" | "self"
        | "super" | "const" | "crate" | "pub" | "in" | "box" | "become" | "virtual"
        | "override" | "priv" | "async" | "await" | "dyn" | "abstract" | "true" | "false"
        | "as" => format!("{}_", s),
        _ => s,
    }
}

fn to_snake_case(s: &str) -> String {
    let mut snake = String::new();
    for (i, ch) in s.chars().enumerate() {
        if i > 0 && ch.is_uppercase() {
            snake.push('_');
        }
        snake.push(ch.to_ascii_lowercase());
    }
    snake.replace('$', "_").replace("__", "_")
}

fn get_type_suffix(desc: &str) -> String {
    let mut suffix = String::from("_");
    let mut chars = desc.chars().peekable();
    if chars.next() != Some('(') {
        return String::new();
    }
    while let Some(&c) = chars.peek() {
        if c == ')' {
            break;
        }
        match chars.next().unwrap() {
            'Z' | 'B' => suffix.push('b'),
            'C' => suffix.push('c'),
            'S' => suffix.push('s'),
            'I' => suffix.push('i'),
            'J' => suffix.push('j'),
            'F' => suffix.push('f'),
            'D' => suffix.push('d'),
            'L' => {
                let mut path = String::new();
                while let Some(ch) = chars.next() {
                    if ch == ';' {
                        break;
                    }
                    path.push(ch);
                }
                let simple = path.split('/').last().unwrap_or("obj");
                suffix.push_str(&to_snake_case(simple));
            }
            '[' => {
                suffix.push('a');
                while chars.peek() == Some(&'[') {
                    chars.next();
                }
                if chars.peek() == Some(&'L') {
                    while chars.next() != Some(';') {}
                } else {
                    chars.next();
                }
            }
            _ => suffix.push('v'),
        }
    }
    if suffix == "_" {
        "_v".to_string()
    } else {
        suffix
    }
}

fn parse_descriptor(desc: &str) -> (Vec<proc_macro2::TokenStream>, proc_macro2::TokenStream) {
    let mut params = Vec::new();
    let mut chars = desc.chars().peekable();
    let map_type =
        |c: &mut std::iter::Peekable<std::str::Chars>| match c.next().expect("Unexpected end") {
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
                while let Some(ch) = c.next() {
                    if ch == ';' {
                        break;
                    }
                }
                quote! { *mut std::ffi::c_void }
            }
            '[' => {
                while let Some(&'[') = c.peek() {
                    c.next();
                }
                match c.peek() {
                    Some('L') => {
                        while let Some(ch) = c.next() {
                            if ch == ';' {
                                break;
                            }
                        }
                    }
                    Some(_) => {
                        c.next();
                    }
                    None => {}
                }
                quote! { *mut std::ffi::c_void }
            }
            _ => quote! { *mut std::ffi::c_void },
        };
    if chars.next() == Some('(') {
        while let Some(&c) = chars.peek() {
            if c == ')' {
                chars.next();
                break;
            }
            params.push(map_type(&mut chars));
        }
    }
    let ret = map_type(&mut chars);
    (params, ret)
}

fn generate_class_code(metadata: &MinecraftMetadata) -> proc_macro2::TokenStream {
    let class_simple_name = metadata.class_name.split('.').last().unwrap();
    let safe_name = safe_ident(class_simple_name);
    let cls_id = format_ident!("{}", safe_name);
    let vt_id = format_ident!("{}VTable", safe_name);
    let mut fields = Vec::new();
    let mut impls = quote! {};
    let mut seen_methods = HashSet::new();

    for m in &metadata.methods {
        let base_name = if m.name.starts_with('$') {
            format!("internal_{}", &m.name[1..])
        } else {
            m.name.clone()
        };
        let suffix = get_type_suffix(&m.descriptor);
        let mut final_name = format!(
            "{}_{}",
            to_snake_case(&base_name),
            suffix.trim_start_matches('_')
        );
        let mut count = 1;
        while !seen_methods.insert(final_name.clone()) {
            final_name = format!(
                "{}_{}_{}",
                to_snake_case(&base_name),
                suffix.trim_start_matches('_'),
                count
            );
            count += 1;
        }
        let id = format_ident!("{}", final_name);
        fields.push(quote! { pub #id: usize });
        let (p_types, r_type) = parse_descriptor(&m.descriptor);
        let args: Vec<_> = (0..p_types.len())
            .map(|i| format_ident!("arg{}", i))
            .collect();
        let mut c_types = Vec::new();
        let mut c_args = Vec::new();
        if !m.is_static {
            c_types.push(quote! { *mut std::ffi::c_void });
            c_args.push(quote! { self.ptr });
        }
        c_types.extend(p_types.iter().cloned());
        c_args.extend(args.iter().map(|a| quote! { #a }));
        impls.extend(quote! {
            pub fn #id(&self, #(#args: #p_types),*) -> #r_type {
                unsafe {
                    let table = #vt_id::get();
                    let func: unsafe extern "C" fn(#(#c_types),*) -> #r_type = std::mem::transmute(table.#id);
                    func(#(#c_args),*)
                }
            }
        });
    }
    quote! {
        use std::sync::OnceLock;
        #[repr(C)] pub struct #vt_id { #(#fields,)* }
        pub static VTABLE: OnceLock<#vt_id> = OnceLock::new();
        impl #vt_id { pub unsafe fn get() -> &'static Self { VTABLE.get().expect("VTable uninit") } }
        pub struct #cls_id { pub ptr: *mut std::ffi::c_void }
        impl #cls_id { pub fn from_raw(ptr: *mut std::ffi::c_void) -> Self { Self { ptr } } #impls }
    }
}

fn main() {
    let json_dir = Path::new("../build/mappings/");
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
    if out_dir.exists() {
        let _ = fs::remove_dir_all(&out_dir);
    }
    fs::create_dir_all(&out_dir).unwrap();

    let all_files: Vec<_> = WalkDir::new(json_dir)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().and_then(|s| s.to_str()) == Some("json"))
        .collect();

    let mut packages: BTreeMap<String, BTreeSet<String>> = BTreeMap::new();
    let mut classes_in_pkg: BTreeMap<String, Vec<MinecraftMetadata>> = BTreeMap::new();

    for entry in &all_files {
        let content = fs::read_to_string(entry.path()).unwrap();
        let metadata: MinecraftMetadata = serde_json::from_str(&content).unwrap();
        let parts: Vec<String> = metadata
            .class_name
            .split('.')
            .map(|s| s.to_string())
            .collect();
        let pkg_key = if parts.len() <= 1 {
            "root".to_string()
        } else {
            parts[..parts.len() - 1].join(".")
        };
        classes_in_pkg
            .entry(pkg_key.clone())
            .or_default()
            .push(metadata);
        for i in 0..parts.len() - 1 {
            let parent = if i == 0 {
                "root".to_string()
            } else {
                parts[..i].join(".")
            };
            packages.entry(parent).or_default().insert(parts[i].clone());
        }
    }

    let all_keys: BTreeSet<_> = packages
        .keys()
        .chain(classes_in_pkg.keys())
        .cloned()
        .collect();

    for key in all_keys {
        // 全ファイルでアウター属性を使用
        let mut content = quote! {
            #[allow(unused_imports, non_camel_case_types, dead_code, non_snake_case, unused_variables)]
        };

        let pkgs = packages.get(&key).cloned().unwrap_or_default();
        let current_classes = classes_in_pkg.get(&key);
        let mut seen_mod_names = HashSet::new();

        if let Some(clss) = current_classes {
            for metadata in clss {
                let class_name = metadata.class_name.split('.').last().unwrap();
                let snake_name = to_snake_case(class_name);
                let safe_name = safe_ident(class_name);
                let cls_id = format_ident!("{}", safe_name);
                let vt_id = format_ident!("{}VTable", safe_name);

                // 全てのクラスをインラインの "クラス名_mod" モジュールに定義
                // これにより、パッケージとしての mod 名とクラスファイルとしての mod 名が衝突しなくなります。
                let mod_name_unique = format!("{}_mod", snake_name);
                let mod_id = format_ident!("{}", mod_name_unique);
                let class_code = generate_class_code(metadata);

                content.extend(quote! {
                    pub mod #mod_id { #class_code }
                    pub use self::#mod_id::{#cls_id, #vt_id};
                });
                seen_mod_names.insert(snake_name);
            }
        }

        for pkg in &pkgs {
            let snake_pkg = to_snake_case(pkg);
            // パッケージとしての mod 宣言
            // seen_mod_names は「クラスそのもの」の名前。パッケージ名と被っていても、
            // クラスは _mod 側にいるので、安心して pub mod snake_pkg; を出力できます。
            let mod_id = format_ident!("{}", snake_pkg);
            content.extend(quote! { pub mod #mod_id; });
        }

        let file_path = if key == "root" {
            out_dir.join("bindings.rs")
        } else {
            out_dir.join(
                key.split('.')
                    .map(to_snake_case)
                    .collect::<Vec<_>>()
                    .join("/")
                    + ".rs",
            )
        };
        if let Some(p) = file_path.parent() {
            fs::create_dir_all(p).unwrap();
        }
        fs::write(&file_path, content.to_string()).unwrap();
    }
    let _ = Command::new("rustfmt")
        .arg("--edition")
        .arg("2024")
        .arg(out_dir.join("bindings.rs"))
        .status();
}
