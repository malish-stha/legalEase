import { ClerkProvider } from "@clerk/nextjs";
import { shadcn } from "@clerk/ui/themes";
import StoreProvider from "../store/StoreProvider";
import "./globals.css";
import { Lora } from "next/font/google";
import { cn } from "@/lib/utils";
import { Toaster } from "sonner";

const lora = Lora({subsets:['latin'],variable:'--font-serif'});

export const metadata = {
  title: "LegalEase Nepal — AI Legal Assistant",
  description: "AI-powered legal document assistant for Nepal. Upload deeds, contracts, and Lalpurja for translation, summary, and risk scoring.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ne" dir="ltr" className={cn("dark", "font-serif", lora.variable)}>
      <body className="antialiased min-h-screen bg-background text-foreground">
        <ClerkProvider appearance={{ baseTheme: shadcn }}>
          <StoreProvider>
            {children}
            <Toaster richColors closeButton position="top-right" />
          </StoreProvider>
        </ClerkProvider>
      </body>
    </html>
  );
}
