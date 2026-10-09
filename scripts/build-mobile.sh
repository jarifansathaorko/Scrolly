#!/bin/bash
set -e

echo "📱 Scrolly Mobile Build Script"
echo "=============================="

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check Node
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js not found. Please install Node 18+${NC}"
    exit 1
fi

echo -e "${BLUE}🔍 Node version: $(node -v)${NC}"
echo -e "${BLUE}🔍 NPM version: $(npm -v)${NC}"

# Step 1: Install deps
echo -e "\n${YELLOW}📦 Step 1: Installing dependencies...${NC}"
npm ci --silent || npm install --silent
echo -e "${GREEN}✅ Dependencies installed${NC}"

# Step 2: Build web
echo -e "\n${YELLOW}🔨 Step 2: Building web assets...${NC}"
npm run build
echo -e "${GREEN}✅ Web build complete (dist/)${NC}"
du -sh dist/

# Step 3: Capacitor sync
echo -e "\n${YELLOW}🔄 Step 3: Syncing to native projects...${NC}"
if [ -d "android" ]; then
    npx cap sync android
    echo -e "${GREEN}✅ Synced to Android${NC}"
else
    echo -e "${YELLOW}⚠️ Android project not found, creating...${NC}"
    npx cap add android
    echo -e "${GREEN}✅ Android project created${NC}"
fi

# Step 4: Android APK (if Java available)
echo -e "\n${YELLOW}🤖 Step 4: Building Android APK...${NC}"
if command -v java &> /dev/null && [ -f "android/gradlew" ]; then
    echo -e "${BLUE}Java found: $(java -version 2>&1 | head -n1)${NC}"
    cd android
    chmod +x gradlew
    echo -e "${BLUE}Running ./gradlew assembleDebug...${NC}"
    ./gradlew assembleDebug --no-daemon
    cd ..
    
    APK_PATH="android/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo -e "${GREEN}✅ APK built: $APK_PATH${NC}"
        cp "$APK_PATH" ./scrolly-debug.apk
        echo -e "${GREEN}📦 Copied to ./scrolly-debug.apk ($(du -h scrolly-debug.apk | cut -f1))${NC}"
        
        # Also build release if requested
        if [ "$1" == "--release" ]; then
            echo -e "\n${YELLOW}🚀 Building release APK...${NC}"
            cd android
            ./gradlew assembleRelease --no-daemon
            cd ..
            echo -e "${GREEN}✅ Release APK built${NC}"
            ls -lh android/app/build/outputs/apk/release/
        fi
    else
        echo -e "${RED}❌ APK not found at $APK_PATH${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️ Java not found or gradlew missing, skipping APK build${NC}"
    echo -e "${YELLOW}   Web assets are ready in dist/ and synced to android/app/src/main/assets/public/${NC}"
    echo -e "${YELLOW}   To build APK, install JDK 17+ and Android SDK, then run:${NC}"
    echo -e "${BLUE}   cd android && ./gradlew assembleDebug${NC}"
    echo -e "${GREEN}✅ Mobile web build complete (APK requires Android SDK)${NC}"
fi

echo -e "\n${GREEN}🎉 Scrolly mobile build finished!${NC}"
echo -e "${BLUE}📁 Web: dist/${NC}"
if [ -f "scrolly-debug.apk" ]; then
    echo -e "${BLUE}📱 APK: scrolly-debug.apk${NC}"
fi
echo -e "${BLUE}💡 Next steps:${NC}"
echo -e "   - Test web: npm run preview"
echo -e "   - Open Android: npx cap open android"
echo -e "   - Run on device: npx cap run android"
echo ""
