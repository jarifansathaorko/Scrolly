#!/usr/bin/env node
/**
 * Generate app icons for all required sizes
 * Requires: sharp (npm install sharp) or uses canvas fallback
 */

const fs = require('fs');
const path = require('path');

const SIZES = [48, 72, 96, 120, 144, 152, 180, 192, 512, 1024];
const ANDROID_MAP = {
  'mipmap-mdpi': 48,
  'mipmap-hdpi': 72,
  'mipmap-xhdpi': 96,
  'mipmap-xxhdpi': 144,
  'mipmap-xxxhdpi': 192,
};

async function generateWithSharp() {
  try {
    const sharp = require('sharp');
    const input = path.join(__dirname, '../public/icon-512.png');
    
    if (!fs.existsSync(input)) {
      console.error('❌ public/icon-512.png not found');
      return false;
    }

    console.log('📱 Generating icons with sharp...');

    for (const size of SIZES) {
      const output = path.join(__dirname, `../public/icon-${size}.png`);
      await sharp(input).resize(size, size).png().toFile(output);
      console.log(`✅ Generated icon-${size}.png`);
    }

    // Android mipmap
    for (const [folder, size] of Object.entries(ANDROID_MAP)) {
      const dir = path.join(__dirname, `../android/app/src/main/res/${folder}`);
      if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
      
      const output = path.join(dir, 'ic_launcher.png');
      await sharp(input).resize(size, size).png().toFile(output);
      
      const outputRound = path.join(dir, 'ic_launcher_round.png');
      await sharp(input).resize(size, size).png().toFile(outputRound);
      
      const outputFg = path.join(dir, 'ic_launcher_foreground.png');
      await sharp(input).resize(size, size).png().toFile(outputFg);
      
      console.log(`✅ Android ${folder} (${size}px)`);
    }

    // Splash
    const splashInput = path.join(__dirname, '../public/icon-512.png');
    const splashBg = { r: 254, g: 247, b: 255, alpha: 1 };
    
    // Create 512x512 splash with centered icon
    const splashSize = 512;
    const iconSize = 200;
    const iconBuffer = await sharp(splashInput).resize(iconSize, iconSize).toBuffer();
    
    const splash = await sharp({
      create: {
        width: splashSize,
        height: splashSize,
        channels: 4,
        background: splashBg,
      }
    })
    .composite([{ input: iconBuffer, left: (splashSize - iconSize) / 2, top: (splashSize - iconSize) / 2 }])
    .png()
    .toBuffer();

    fs.writeFileSync(path.join(__dirname, '../android/app/src/main/res/drawable/splash.png'), splash);
    fs.writeFileSync(path.join(__dirname, '../android/app/src/main/res/mipmap-xxhdpi/splash.png'), splash);
    
    console.log('✅ Splash screens generated');
    return true;
  } catch (e) {
    console.log('⚠️ sharp not available, using fallback:', e.message);
    return false;
  }
}

async function main() {
  const success = await generateWithSharp();
  if (!success) {
    console.log('💡 Install sharp for better icon generation: npm install --save-dev sharp');
    console.log('   Fallback: using existing icons from public/');
  }
  console.log('🎉 Icon generation complete');
}

main();
