import type { NextConfig } from "next";
import path from "path";

const isDesktop = !!process.env.DESKTOP_BUILD;
const isStandaloneServerBuild = process.env.NEXT_STANDALONE_BUILD === '1';

const nextConfig: NextConfig = {
  output: isDesktop ? 'export' : isStandaloneServerBuild ? 'standalone' : undefined,
  turbopack: {
    root: process.cwd(),
  },
  images: {
    unoptimized: isDesktop,
  },
  webpack: (config) => {
    // When building for non-desktop targets (Android APK, web),
    // replace desktop-specific modules with lightweight stubs
    // to exclude Tauri APIs and desktop-only code from the bundle.
    if (!isDesktop) {
      config.resolve.alias = {
        ...config.resolve.alias,
        '@/lib/desktop-runtime': path.resolve(__dirname, 'src/lib/desktop-runtime.stub'),
        '@/lib/desktop-config': path.resolve(__dirname, 'src/lib/desktop-config.stub'),
      };
    }
    return config;
  },
};

export default nextConfig;
