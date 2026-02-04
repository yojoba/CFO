/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  swcMinify: true,
  // Allow cross-origin requests from production domain
  allowedDevOrigins: ['cfoweb.flowbiz.ai'],
  // Performance optimizations
  compress: true,
  poweredByHeader: false,
  // Disable ESLint during builds (can be fixed later)
  eslint: {
    ignoreDuringBuilds: true,
  },
  // Optimize images
  images: {
    formats: ['image/avif', 'image/webp'],
  },
}

module.exports = nextConfig
