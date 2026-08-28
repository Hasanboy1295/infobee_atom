import './globals.css';
import { AuthProvider } from '@/components/AuthProvider';
import { ToastProvider } from '@/components/Toast';
import { LanguageProvider } from '@/components/LanguageProvider';

export const metadata = {
  title: 'ATOM Platform',
  description: 'Admin + User + AI platform',
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body>
        <LanguageProvider>
          <ToastProvider>
            <AuthProvider>{children}</AuthProvider>
          </ToastProvider>
        </LanguageProvider>
      </body>
    </html>
  );
}
