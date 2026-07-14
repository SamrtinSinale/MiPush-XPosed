#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== MiPush-XPosed Release Build ==="

# Check keystore
if [ ! -f release.keystore ]; then
    echo "[!] release.keystore 不存在，正在生成..."
    /d/mobie/Java/jdk-24/bin/keytool -genkey -v \
        -keystore release.keystore \
        -alias mykey \
        -keyalg RSA -keysize 2048 \
        -validity 10000 \
        -storepass 123456 -keypass 123456 \
        -dname "CN=MiPush, OU=Dev, O=MiPush-XPosed, L=Unknown, ST=Unknown, C=CN"
    echo "[+] release.keystore 已生成"
fi

# Check local.properties
if [ ! -f local.properties ]; then
    echo "[!] local.properties 不存在，正在创建..."
    cat > local.properties << 'EOF'
STORE_FILE_PATH=release.keystore
STORE_PASSWORD=123456
KEY_ALIAS=mykey
KEY_PASSWORD=123456
EOF
    echo "[+] local.properties 已创建"
fi

echo "[*] 开始编译..."
./gradlew clean :app:assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK" ]; then
    echo ""
    echo "=== 编译成功 ==="
    echo "APK: $APK"
    echo "大小: $(du -h "$APK" | cut -f1)"
    echo ""
    echo "安装命令:"
    echo "  adb install $APK"
else
    echo "[!] 编译失败，未找到 APK"
    exit 1
fi
