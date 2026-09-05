import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  metadataBase: new URL('https://offerpilot-ai.merry-smile-0923.chatgpt.site'),
  title: 'OfferPilot · 智能求职决策助手',
  description: '用可信数据和可解释分析，做出更适合你的秋招选择。',
  openGraph: {
    title: 'OfferPilot · 智能求职决策助手',
    description: '让每一个 Offer，都有据可选。',
    images: ['/og.png'],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'OfferPilot · 智能求职决策助手',
    description: '让每一个 Offer，都有据可选。',
    images: ['/og.png'],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
