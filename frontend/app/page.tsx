"use client";

import Link from "next/link";
import { SignInButton, SignUpButton, SignedIn, SignedOut, UserButton } from "@clerk/nextjs";
import { useSelector } from "react-redux";
import { RootState } from "../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

export default function Home() {
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  return (
    <div className="flex flex-col min-h-screen bg-background text-foreground overflow-hidden">
      {/* Navigation Header */}
      <header className="border-b border-muted bg-background/90 backdrop-blur-md sticky top-0 z-50 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-2xl font-serif font-bold text-primary tracking-wide">⚖️ LegalEase</span>
          <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-foreground/80 border border-muted">{t.nepal}</span>
        </div>

        <nav className="flex items-center gap-6">
          <LanguageToggler />

          <SignedOut>
            <SignInButton mode="modal">
              <button className="text-sm font-medium hover:text-primary transition-colors cursor-pointer">{t.login}</button>
            </SignInButton>
            <SignUpButton mode="modal">
              <button className="bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-semibold px-4 py-2 rounded-md transition-all shadow-md cursor-pointer">
                {t.register}
              </button>
            </SignUpButton>
          </SignedOut>

          <SignedIn>
            <Link
              href="/dashboard"
              className="bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-semibold px-4 py-2 rounded-md transition-all shadow-md cursor-pointer"
            >
              {t.dashboard}
            </Link>
            <UserButton afterSignOutUrl="/" />
          </SignedIn>
        </nav>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col items-center justify-center text-center px-6 py-12 max-w-4xl mx-auto">
        <div className="space-y-6">
          <h1 className="text-5xl md:text-6xl font-serif font-bold tracking-tight text-foreground leading-tight">
            {language === "ne" ? (
              <>
                कानूनी दस्तावेजहरूलाई <span className="text-primary">सरल भाषामा</span> बुझ्नुहोस्
              </>
            ) : (
              <>
                Understand Legal Documents in <span className="text-primary">Simple Language</span>
              </>
            )}
          </h1>
          <p className="text-lg md:text-xl text-muted-foreground font-light max-w-2xl mx-auto">
            {t.subtitle}
          </p>

          <div className="pt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
            <SignedOut>
              <SignUpButton mode="modal">
                <button className="bg-primary hover:bg-primary/90 text-primary-foreground text-lg font-semibold px-8 py-3 rounded-lg shadow-lg hover:shadow-xl transition-all cursor-pointer">
                  {t.getStarted}
                </button>
              </SignUpButton>
            </SignedOut>

            <SignedIn>
              <Link href="/dashboard">
                <button className="bg-primary hover:bg-primary/90 text-primary-foreground text-lg font-semibold px-8 py-3 rounded-lg shadow-lg hover:shadow-xl transition-all cursor-pointer">
                  {t.goToDashboard}
                </button>
              </Link>
            </SignedIn>

            <Link href="/lawyers">
              <button className="bg-muted hover:bg-muted/80 border border-muted text-foreground text-lg font-semibold px-8 py-3 rounded-lg shadow-md transition-all cursor-pointer">
                {t.findLawyers}
              </button>
            </Link>
          </div>
        </div>

        {/* Dynamic Feature Highlights */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full mt-24">
          <div className="bg-card border border-muted p-6 rounded-xl text-left">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center text-2xl mb-4 text-primary">📄</div>
            <h3 className="text-xl font-serif font-bold text-foreground mb-2">{t.aiAnalysis}</h3>
            <p className="text-muted-foreground text-sm">
              {t.aiAnalysisDesc}
            </p>
          </div>

          <div className="bg-card border border-muted p-6 rounded-xl text-left">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center text-2xl mb-4 text-primary">🗣️</div>
            <h3 className="text-xl font-serif font-bold text-foreground mb-2">{t.translation}</h3>
            <p className="text-muted-foreground text-sm">
              {t.translationDesc}
            </p>
          </div>

          <div className="bg-card border border-muted p-6 rounded-xl text-left">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center text-2xl mb-4 text-primary">👨‍⚖️</div>
            <h3 className="text-xl font-serif font-bold text-foreground mb-2">{t.consultation}</h3>
            <p className="text-muted-foreground text-sm">
              {t.consultationDesc}
            </p>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-muted py-6 text-center text-muted-foreground text-xs">
        <p>© {new Date().getFullYear()} LegalEase Nepal. All rights reserved.</p>
      </footer>
    </div>
  );
}
