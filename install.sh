#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
#  Infinite Client Installer for macOS
#  Version 2.1.0 | Minecraft 1.21.11 | Requires JDK 25
# ═══════════════════════════════════════════════════════════════════════════════

# ─────────────────────────────────────────────────────────────────────────────────
# ANSI Color Codes & Styling
# ─────────────────────────────────────────────────────────────────────────────────
RESET='\033[0m'
BOLD='\033[1m'
DIM='\033[2m'
ITALIC='\033[3m'
UNDERLINE='\033[4m'
BLINK='\033[5m'

# Regular Colors
BLACK='\033[0;30m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'

# Bold Colors
BRED='\033[1;31m'
BGREEN='\033[1;32m'
BYELLOW='\033[1;33m'
BBLUE='\033[1;34m'
BPURPLE='\033[1;35m'
BCYAN='\033[1;36m'
BWHITE='\033[1;37m'

# Background Colors
BG_BLACK='\033[40m'
BG_RED='\033[41m'
BG_GREEN='\033[42m'
BG_YELLOW='\033[43m'
BG_BLUE='\033[44m'
BG_PURPLE='\033[45m'
BG_CYAN='\033[46m'
BG_WHITE='\033[47m'

# Gradient colors (256 color mode)
GRAD1='\033[38;5;39m'   # Light blue
GRAD2='\033[38;5;45m'   # Cyan
GRAD3='\033[38;5;51m'   # Bright cyan
GRAD4='\033[38;5;87m'   # Light cyan
GRAD5='\033[38;5;123m'  # Pale cyan
ORANGE='\033[38;5;208m'
PINK='\033[38;5;213m'
LIME='\033[38;5;118m'

# ─────────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────────
MINECRAFT_DIR="$HOME/Library/Application Support/minecraft"
MODS_DIR="$MINECRAFT_DIR/mods"
JDK_VERSION="25"
JDK_INSTALL_DIR="/Library/Java/JavaVirtualMachines"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOD_VERSION="2.1.0+1.21.11"
MINECRAFT_VERSION="1.21.11"
FABRIC_LOADER_VERSION="0.18.2"
FABRIC_API_VERSION="0.139.5+1.21.11"
FABRIC_KOTLIN_VERSION="1.13.7+kotlin.2.2.21"

# ─────────────────────────────────────────────────────────────────────────────────
# Helper Functions
# ─────────────────────────────────────────────────────────────────────────────────

# Clear screen and hide cursor
init_display() {
    clear
    tput civis  # Hide cursor
    trap 'tput cnorm; echo -e "${RESET}"; exit' INT TERM EXIT
}

# Show cursor on exit
cleanup() {
    tput cnorm
    echo -e "${RESET}"
}

# Get terminal dimensions
get_term_size() {
    TERM_COLS=$(tput cols)
    TERM_ROWS=$(tput lines)
}

# Center text
center_text() {
    local text="$1"
    local clean_text=$(echo -e "$text" | sed 's/\x1B\[[0-9;]*m//g')
    local text_len=${#clean_text}
    local padding=$(( (TERM_COLS - text_len) / 2 ))
    printf "%${padding}s" ""
    echo -e "$text"
}

# Print horizontal line
print_line() {
    local char="${1:-─}"
    local color="${2:-$DIM$CYAN}"
    echo -e "${color}$(printf '%*s' "$TERM_COLS" '' | tr ' ' "$char")${RESET}"
}

# Print double line
print_double_line() {
    echo -e "${GRAD2}$(printf '%*s' "$TERM_COLS" '' | tr ' ' '═')${RESET}"
}

# Animated spinner
spinner() {
    local pid=$1
    local message="$2"
    local spinners=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    local i=0
    
    while kill -0 $pid 2>/dev/null; do
        printf "\r${BCYAN}  ${spinners[$i]} ${BWHITE}${message}${RESET}"
        i=$(( (i + 1) % ${#spinners[@]} ))
        sleep 0.1
    done
    printf "\r"
}

# Progress bar
progress_bar() {
    local current=$1
    local total=$2
    local width=40
    local percentage=$((current * 100 / total))
    local filled=$((current * width / total))
    local empty=$((width - filled))
    
    # Color gradient based on progress
    local color
    if [ $percentage -lt 33 ]; then
        color=$RED
    elif [ $percentage -lt 66 ]; then
        color=$YELLOW
    else
        color=$GREEN
    fi
    
    printf "\r  ${DIM}[${RESET}"
    printf "${color}%${filled}s" '' | tr ' ' '█'
    printf "${DIM}%${empty}s" '' | tr ' ' '░'
    printf "${DIM}]${RESET} ${BWHITE}%3d%%${RESET}" $percentage
}

# Animated progress for tasks
animate_progress() {
    local message="$1"
    local duration="${2:-2}"
    local steps=20
    
    echo ""
    for ((i=1; i<=steps; i++)); do
        progress_bar $i $steps
        printf " ${CYAN}${message}${RESET}"
        sleep $(echo "scale=2; $duration / $steps" | bc)
    done
    echo ""
}

# Success message with checkmark
success() {
    echo -e "  ${BGREEN}✓${RESET} ${GREEN}$1${RESET}"
}

# Error message with X
error() {
    echo -e "  ${BRED}✗${RESET} ${RED}$1${RESET}"
}

# Warning message
warning() {
    echo -e "  ${BYELLOW}⚠${RESET} ${YELLOW}$1${RESET}"
}

# Info message
info() {
    echo -e "  ${BCYAN}ℹ${RESET} ${CYAN}$1${RESET}"
}

# Step header
step_header() {
    local step_num=$1
    local title="$2"
    echo ""
    print_line "─" "$GRAD2"
    center_text "${GRAD3}${BOLD}[ STEP ${step_num} ]${RESET} ${BWHITE}${title}${RESET}"
    print_line "─" "$GRAD2"
    echo ""
}

# Box drawing
draw_box() {
    local title="$1"
    local content="$2"
    local width=60
    local padding=$(( (TERM_COLS - width) / 2 ))
    local pad_str=$(printf '%*s' "$padding" '')
    
    echo -e "${pad_str}${GRAD2}╭$(printf '─%.0s' $(seq 1 $((width-2))))╮${RESET}"
    echo -e "${pad_str}${GRAD2}│${RESET}${BOLD}${BWHITE}$(printf "%-$((width-2))s" "  $title")${RESET}${GRAD2}│${RESET}"
    echo -e "${pad_str}${GRAD2}├$(printf '─%.0s' $(seq 1 $((width-2))))┤${RESET}"
    
    while IFS= read -r line; do
        local clean_line=$(echo -e "$line" | sed 's/\x1B\[[0-9;]*m//g')
        local line_len=${#clean_line}
        local spaces=$((width - 4 - line_len))
        if [ $spaces -lt 0 ]; then spaces=0; fi
        echo -e "${pad_str}${GRAD2}│${RESET}  ${line}$(printf '%*s' "$spaces" '')${GRAD2}│${RESET}"
    done <<< "$content"
    
    echo -e "${pad_str}${GRAD2}╰$(printf '─%.0s' $(seq 1 $((width-2))))╯${RESET}"
}

# Typing effect
type_text() {
    local text="$1"
    local delay="${2:-0.02}"
    
    for ((i=0; i<${#text}; i++)); do
        printf "${text:$i:1}"
        sleep $delay
    done
    echo ""
}

# Yes/No prompt
prompt_yn() {
    local message="$1"
    local default="${2:-y}"
    local response
    
    if [ "$default" = "y" ]; then
        echo -en "  ${BPURPLE}?${RESET} ${BWHITE}${message}${RESET} ${DIM}[Y/n]${RESET} "
    else
        echo -en "  ${BPURPLE}?${RESET} ${BWHITE}${message}${RESET} ${DIM}[y/N]${RESET} "
    fi
    
    read -r response
    response=${response:-$default}
    
    case "$response" in
        [Yy]* ) return 0 ;;
        * ) return 1 ;;
    esac
}

# ─────────────────────────────────────────────────────────────────────────────────
# ASCII Art Logo
# ─────────────────────────────────────────────────────────────────────────────────

print_logo() {
    echo ""
    echo ""
    center_text "${GRAD1} ___ _   _ _____ ___ _   _ ___ _____ _____${RESET}"
    center_text "${GRAD2}|_ _| \\ | |  ___|_ _| \\ | |_ _|_   _| ____|${RESET}"
    center_text "${GRAD3} | ||  \\| | |_   | ||  \\| || |  | | |  _|${RESET}"
    center_text "${GRAD4} | || |\\  |  _|  | || |\\  || |  | | | |___|${RESET}"
    center_text "${GRAD5}|___|_| \\_|_|   |___|_| \\_|___| |_| |_____|${RESET}"
    echo ""
    center_text "${GRAD2}  ____ _     ___ _____ _   _ _____${RESET}"
    center_text "${GRAD2} / ___| |   |_ _| ____| \\ | |_   _|${RESET}"
    center_text "${GRAD3}| |   | |    | ||  _| |  \\| | | |${RESET}"
    center_text "${GRAD4}| |___| |___ | || |___| |\\  | | |${RESET}"
    center_text "${GRAD5} \\____|_____|___|_____|_| \\_| |_|${RESET}"
    echo ""
    center_text "${DIM}======================================================${RESET}"
    center_text "${BWHITE}Version ${MOD_VERSION}${RESET} ${DIM}|${RESET} ${CYAN}Minecraft ${MINECRAFT_VERSION}${RESET} ${DIM}|${RESET} ${PURPLE}JDK ${JDK_VERSION}${RESET}"
    center_text "${DIM}======================================================${RESET}"
    echo ""
}

# ─────────────────────────────────────────────────────────────────────────────────
# System Detection
# ─────────────────────────────────────────────────────────────────────────────────

detect_system() {
    step_header "1" "System Detection"
    
    # Detect architecture
    ARCH=$(uname -m)
    if [ "$ARCH" = "arm64" ]; then
        ARCH_DISPLAY="Apple Silicon (ARM64)"
        ARCH_TYPE="aarch64"
        JDK_PLATFORM="macos-aarch64"
    else
        ARCH_DISPLAY="Intel (x86_64)"
        ARCH_TYPE="x64"
        JDK_PLATFORM="macos-x64"
    fi
    
    # Detect macOS version
    MACOS_VERSION=$(sw_vers -productVersion)
    MACOS_NAME=$(awk '/SOFTWARE LICENSE AGREEMENT FOR macOS/' '/System/Library/CoreServices/Setup Assistant.app/Contents/Resources/en.lproj/OSXSoftwareLicense.rtf' 2>/dev/null | awk -F 'macOS ' '{print $NF}' | awk '{print $1}' || echo "macOS")
    
    # Create info display
    local sys_info="${BCYAN}◆${RESET} Operating System: ${BWHITE}macOS ${MACOS_VERSION}${RESET}
${BCYAN}◆${RESET} Architecture: ${BWHITE}${ARCH_DISPLAY}${RESET}
${BCYAN}◆${RESET} Hostname: ${BWHITE}$(hostname)${RESET}
${BCYAN}◆${RESET} User: ${BWHITE}$(whoami)${RESET}"
    
    draw_box "System Information" "$sys_info"
    echo ""
    success "System detected successfully"
}

# ─────────────────────────────────────────────────────────────────────────────────
# Check Prerequisites
# ─────────────────────────────────────────────────────────────────────────────────

check_prerequisites() {
    step_header "2" "Checking Prerequisites"
    
    local all_good=true
    
    # Check for Minecraft installation
    echo -e "  ${BCYAN}●${RESET} Checking Minecraft installation..."
    sleep 0.5
    if [ -d "$MINECRAFT_DIR" ]; then
        success "Minecraft directory found"
    else
        warning "Minecraft directory not found - will be created"
    fi
    
    # Check for existing Java installations
    echo -e "  ${BCYAN}●${RESET} Scanning for Java installations..."
    sleep 0.5
    
    if [ -d "$JDK_INSTALL_DIR" ]; then
        local java_versions=$(ls "$JDK_INSTALL_DIR" 2>/dev/null | grep -E "jdk|openjdk" || echo "")
        if [ -n "$java_versions" ]; then
            info "Found existing Java installations:"
            echo "$java_versions" | while read -r jdk; do
                echo -e "    ${DIM}└─${RESET} ${WHITE}$jdk${RESET}"
            done
        fi
    fi
    
    # Check for JDK 25 specifically
    JDK25_PATH=""
    if [ -d "$JDK_INSTALL_DIR/jdk-25.jdk" ]; then
        JDK25_PATH="$JDK_INSTALL_DIR/jdk-25.jdk"
        success "JDK 25 already installed"
        JDK25_INSTALLED=true
    elif [ -d "$JDK_INSTALL_DIR/openjdk-25.jdk" ]; then
        JDK25_PATH="$JDK_INSTALL_DIR/openjdk-25.jdk"
        success "OpenJDK 25 already installed (Homebrew)"
        JDK25_INSTALLED=true
    else
        warning "JDK 25 not found - will be installed"
        JDK25_INSTALLED=false
    fi
    
    # Check for curl
    echo -e "  ${BCYAN}●${RESET} Checking for required tools..."
    sleep 0.3
    if command -v curl &> /dev/null; then
        success "curl is available"
    else
        error "curl is not installed"
        all_good=false
    fi
    
    # Check for Homebrew (optional)
    if command -v brew &> /dev/null; then
        HOMEBREW_AVAILABLE=true
        success "Homebrew is available"
    else
        HOMEBREW_AVAILABLE=false
        info "Homebrew not found (optional)"
    fi
    
    echo ""
    if [ "$all_good" = false ]; then
        error "Some prerequisites are missing. Please install them and try again."
        exit 1
    fi
}

# ─────────────────────────────────────────────────────────────────────────────────
# Install JDK 25
# ─────────────────────────────────────────────────────────────────────────────────

install_jdk25() {
    step_header "3" "Installing JDK 25"
    
    if [ "$JDK25_INSTALLED" = true ]; then
        success "JDK 25 is already installed at: ${BWHITE}${JDK25_PATH}${RESET}"
        echo ""
        
        # Verify the installation
        local java_version=$("$JDK25_PATH/Contents/Home/bin/java" --version 2>&1 | head -1)
        info "Version: ${BWHITE}${java_version}${RESET}"
        return 0
    fi
    
    echo ""
    info "JDK 25 is required for Infinite Client"
    echo ""
    
    # Installation method selection
    echo -e "  ${BPURPLE}?${RESET} ${BWHITE}Choose installation method:${RESET}"
    echo ""
    echo -e "    ${BCYAN}[1]${RESET} ${WHITE}Download from OpenJDK (Recommended)${RESET}"
    echo -e "    ${BCYAN}[2]${RESET} ${WHITE}Install via Homebrew${RESET}"
    echo -e "    ${BCYAN}[3]${RESET} ${WHITE}Skip (I'll install manually)${RESET}"
    echo ""
    echo -en "  ${DIM}Enter choice [1-3]:${RESET} "
    read -r choice
    
    case "$choice" in
        1)
            install_jdk_direct
            ;;
        2)
            install_jdk_homebrew
            ;;
        3)
            warning "Skipping JDK installation"
            warning "Please install JDK 25 manually before running Minecraft"
            return 0
            ;;
        *)
            warning "Invalid choice, using direct download"
            install_jdk_direct
            ;;
    esac
}

install_jdk_direct() {
    echo ""
    info "Downloading JDK 25 from Oracle..."
    echo ""
    
    # Use the official Oracle JDK 25 GA release URL
    local download_url="https://download.oracle.com/java/25/latest/jdk-25_${JDK_PLATFORM}_bin.tar.gz"
    local temp_dir=$(mktemp -d)
    local archive_path="$temp_dir/jdk25.tar.gz"
    
    # Download with progress
    echo -e "  ${BCYAN}↓${RESET} ${WHITE}Downloading...${RESET}"
    if curl -L --progress-bar -o "$archive_path" "$download_url" 2>&1; then
        success "Download complete"
    else
        error "Download failed"
        rm -rf "$temp_dir"
        return 1
    fi
    
    # Extract
    echo -e "  ${BCYAN}📦${RESET} ${WHITE}Extracting...${RESET}"
    tar -xzf "$archive_path" -C "$temp_dir"
    
    # Find the extracted JDK directory
    local jdk_dir=$(find "$temp_dir" -maxdepth 1 -type d -name "jdk-*" | head -1)
    
    if [ -z "$jdk_dir" ]; then
        error "Could not find extracted JDK directory"
        rm -rf "$temp_dir"
        return 1
    fi
    
    # Move to JavaVirtualMachines (requires sudo)
    echo ""
    warning "Administrator privileges required to install JDK system-wide"
    echo ""
    
    if sudo mv "$jdk_dir" "$JDK_INSTALL_DIR/jdk-25.jdk"; then
        success "JDK 25 installed to ${BWHITE}$JDK_INSTALL_DIR/jdk-25.jdk${RESET}"
        JDK25_PATH="$JDK_INSTALL_DIR/jdk-25.jdk"
    else
        error "Failed to install JDK"
        rm -rf "$temp_dir"
        return 1
    fi
    
    rm -rf "$temp_dir"
    
    # Verify installation
    echo ""
    local java_version=$("$JDK25_PATH/Contents/Home/bin/java" --version 2>&1 | head -1)
    success "Installed: ${BWHITE}${java_version}${RESET}"
}

install_jdk_homebrew() {
    echo ""
    
    if [ "$HOMEBREW_AVAILABLE" = false ]; then
        error "Homebrew is not installed"
        info "Install it from: https://brew.sh"
        return 1
    fi
    
    info "Installing JDK 25 via Homebrew..."
    echo ""
    
    # Check if the cask exists
    if brew install --cask openjdk@25 2>&1; then
        success "JDK 25 installed via Homebrew"
        
        # Create symlink
        info "Creating symlink..."
        sudo ln -sfn /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk "$JDK_INSTALL_DIR/openjdk-25.jdk"
        JDK25_PATH="$JDK_INSTALL_DIR/openjdk-25.jdk"
        success "Symlink created"
    else
        error "Failed to install via Homebrew"
        info "Falling back to direct download..."
        install_jdk_direct
    fi
}

# ─────────────────────────────────────────────────────────────────────────────────
# Install Fabric
# ─────────────────────────────────────────────────────────────────────────────────

install_fabric() {
    step_header "4" "Installing Fabric Loader"
    
    local fabric_installer_url="https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar"
    local temp_dir=$(mktemp -d)
    local installer_path="$temp_dir/fabric-installer.jar"
    
    info "Fabric Loader is required to run Infinite Client"
    echo ""
    
    # Check if Fabric is already installed
    local fabric_profile="$MINECRAFT_DIR/versions/fabric-loader-${FABRIC_LOADER_VERSION}-${MINECRAFT_VERSION}"
    if [ -d "$fabric_profile" ]; then
        success "Fabric Loader ${FABRIC_LOADER_VERSION} for Minecraft ${MINECRAFT_VERSION} is already installed"
        rm -rf "$temp_dir"
        return 0
    fi
    
    # Download Fabric installer
    echo -e "  ${BCYAN}↓${RESET} ${WHITE}Downloading Fabric Installer...${RESET}"
    if curl -L --progress-bar -o "$installer_path" "$fabric_installer_url" 2>&1; then
        success "Download complete"
    else
        error "Failed to download Fabric installer"
        rm -rf "$temp_dir"
        return 1
    fi
    
    # Run installer
    echo ""
    info "Running Fabric Installer..."
    echo ""
    
    # Use JDK 25 to run the installer if available
    local java_exec="java"
    if [ -n "$JDK25_PATH" ]; then
        java_exec="$JDK25_PATH/Contents/Home/bin/java"
    fi
    
    if "$java_exec" -jar "$installer_path" client -mcversion "$MINECRAFT_VERSION" -loader "$FABRIC_LOADER_VERSION" -noprofile 2>&1 | while read -r line; do
        echo -e "    ${DIM}${line}${RESET}"
    done; then
        success "Fabric Loader installed successfully"
    else
        warning "Fabric installer may have encountered issues"
        info "You may need to run the Fabric installer manually"
        info "Download from: https://fabricmc.net/use/installer/"
    fi
    
    rm -rf "$temp_dir"
}

# ─────────────────────────────────────────────────────────────────────────────────
# Install Mods
# ─────────────────────────────────────────────────────────────────────────────────

install_mods() {
    step_header "5" "Installing Mods"
    
    # Create mods directory
    if [ ! -d "$MODS_DIR" ]; then
        mkdir -p "$MODS_DIR"
        success "Created mods directory"
    fi
    
    echo ""
    info "Installing required mods..."
    echo ""
    
    # Look for the mod JAR in the script directory or build directory
    local mod_jar=""
    
    # Check build/libs first
    if [ -d "$SCRIPT_DIR/build/libs" ]; then
        mod_jar=$(find "$SCRIPT_DIR/build/libs" -name "infinite-client-*.jar" ! -name "*-sources.jar" ! -name "*-dev.jar" | head -1)
    fi
    
    # Check script directory
    if [ -z "$mod_jar" ]; then
        mod_jar=$(find "$SCRIPT_DIR" -maxdepth 1 -name "infinite-client-*.jar" | head -1)
    fi
    
    if [ -n "$mod_jar" ]; then
        local mod_filename=$(basename "$mod_jar")
        echo -e "  ${BCYAN}📦${RESET} Installing ${BWHITE}$mod_filename${RESET}..."
        cp "$mod_jar" "$MODS_DIR/"
        success "Infinite Client installed"
    else
        warning "Infinite Client JAR not found"
        info "Please build the project first with: ./gradlew build"
        info "Or manually copy the JAR to: $MODS_DIR"
    fi
    
    # Download Fabric API
    echo ""
    echo -e "  ${BCYAN}↓${RESET} ${WHITE}Downloading Fabric API...${RESET}"
    local fabric_api_url="https://cdn.modrinth.com/data/P7dR8mSH/versions/WwaxFBYE/fabric-api-0.139.5%2B1.21.11.jar"
    
    if curl -L --progress-bar -o "$MODS_DIR/fabric-api-${FABRIC_API_VERSION}.jar" "$fabric_api_url" 2>&1; then
        success "Fabric API installed"
    else
        warning "Failed to download Fabric API"
        info "Please download manually from: https://modrinth.com/mod/fabric-api"
    fi
    
    # Download Fabric Language Kotlin
    echo ""
    echo -e "  ${BCYAN}↓${RESET} ${WHITE}Downloading Fabric Language Kotlin...${RESET}"
    local kotlin_url="https://cdn.modrinth.com/data/Ha28R6CL/versions/vI0Kpbbn/fabric-language-kotlin-1.13.7%2Bkotlin.2.2.21.jar"
    
    if curl -L --progress-bar -o "$MODS_DIR/fabric-language-kotlin-${FABRIC_KOTLIN_VERSION}.jar" "$kotlin_url" 2>&1; then
        success "Fabric Language Kotlin installed"
    else
        warning "Failed to download Fabric Language Kotlin"
        info "Please download manually from: https://modrinth.com/mod/fabric-language-kotlin"
    fi
    
    echo ""
    info "Mods installed to: ${BWHITE}$MODS_DIR${RESET}"
}

# ─────────────────────────────────────────────────────────────────────────────────
# Configure Launcher
# ─────────────────────────────────────────────────────────────────────────────────

configure_launcher() {
    step_header "6" "Launcher Configuration"
    
    local java_path=""
    if [ -n "$JDK25_PATH" ]; then
        java_path="$JDK25_PATH/Contents/Home/bin/java"
    else
        java_path="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java"
    fi
    
    echo ""
    draw_box "Minecraft Launcher Settings" "$(echo -e "${BCYAN}Java Executable:${RESET}
${WHITE}$java_path${RESET}

${BCYAN}JVM Arguments:${RESET}
${WHITE}-Xmx4G --enable-native-access=ALL-UNNAMED${RESET}
${WHITE}-Dforeign.restricted=permit${RESET}")"
    
    echo ""
    echo ""
    info "To configure the Minecraft Launcher:"
    echo ""
    echo -e "    ${BWHITE}1.${RESET} Open the Minecraft Launcher"
    echo -e "    ${BWHITE}2.${RESET} Go to ${BCYAN}Installations${RESET} tab"
    echo -e "    ${BWHITE}3.${RESET} Edit the ${BCYAN}fabric-loader-${MINECRAFT_VERSION}${RESET} profile"
    echo -e "    ${BWHITE}4.${RESET} Click ${BCYAN}More Options${RESET}"
    echo -e "    ${BWHITE}5.${RESET} Set ${BCYAN}Java Executable${RESET} to the path shown above"
    echo -e "    ${BWHITE}6.${RESET} Add the ${BCYAN}JVM Arguments${RESET} shown above"
    echo -e "    ${BWHITE}7.${RESET} Click ${BCYAN}Save${RESET}"
    echo ""
    
    # Copy path to clipboard
    if command -v pbcopy &> /dev/null; then
        echo "$java_path" | pbcopy
        success "Java path copied to clipboard!"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────────
# Installation Summary
# ─────────────────────────────────────────────────────────────────────────────────

print_summary() {
    echo ""
    print_double_line
    echo ""
    center_text "${BGREEN}✨ Installation Complete! ✨${RESET}"
    echo ""
    print_double_line
    echo ""
    
    local summary="${BGREEN}✓${RESET} JDK 25 installed
${BGREEN}✓${RESET} Fabric Loader installed
${BGREEN}✓${RESET} Infinite Client mod installed
${BGREEN}✓${RESET} Dependencies installed

${BCYAN}Next Steps:${RESET}
${WHITE}1. Open Minecraft Launcher${RESET}
${WHITE}2. Configure Java executable${RESET}
${WHITE}3. Select fabric-loader profile${RESET}
${WHITE}4. Click Play!${RESET}"
    
    draw_box "Installation Summary" "$summary"
    
    echo ""
    echo ""
    center_text "${DIM}Thank you for installing Infinite Client!${RESET}"
    center_text "${GRAD3}Happy gaming! 🎮${RESET}"
    echo ""
    center_text "${DIM}──────────────────────────────────────${RESET}"
    center_text "${GRAD2}Made by ${BWHITE}The Infinity's${RESET}${GRAD2}, ${BWHITE}rxnd0m_dev${RESET}${GRAD2}, and ${BWHITE}NekomiRyan${RESET}"
    center_text "${DIM}──────────────────────────────────────${RESET}"
    echo ""
    echo ""
}

# ─────────────────────────────────────────────────────────────────────────────────
# Main Entry Point
# ─────────────────────────────────────────────────────────────────────────────────

main() {
    init_display
    get_term_size
    
    print_logo
    
    sleep 1
    
    if ! prompt_yn "Ready to install Infinite Client?"; then
        echo ""
        info "Installation cancelled"
        cleanup
        exit 0
    fi
    
    detect_system
    
    if ! prompt_yn "Continue to check prerequisites?"; then
        info "Installation cancelled"
        cleanup
        exit 0
    fi
    
    check_prerequisites
    
    # Only ask about JDK if it's not already installed
    if [ "$JDK25_INSTALLED" = true ]; then
        if ! prompt_yn "JDK 25 is already installed. Continue to Fabric Loader?"; then
            info "Skipping remaining steps"
            cleanup
            exit 0
        fi
    else
        if ! prompt_yn "Continue to install JDK 25?"; then
            info "Skipping remaining steps"
            cleanup
            exit 0
        fi
        install_jdk25
    fi
    
    if ! prompt_yn "Continue to install Fabric Loader?"; then
        info "Skipping remaining steps"
        cleanup
        exit 0
    fi
    
    install_fabric
    
    if ! prompt_yn "Continue to install mods?"; then
        info "Skipping remaining steps"
        cleanup
        exit 0
    fi
    
    install_mods
    
    if ! prompt_yn "Continue to view launcher configuration?"; then
        info "Skipping remaining steps"
        cleanup
        exit 0
    fi
    
    configure_launcher
    
    echo ""
    if ! prompt_yn "Have you configured the Java executable and JVM arguments in the Minecraft Launcher?"; then
        echo ""
        warning "Please configure the launcher settings shown above before playing."
        info "You can re-run this installer anytime to see the settings again."
        echo ""
    fi
    
    print_summary
    
    cleanup
}

# Run installer
main "$@"
