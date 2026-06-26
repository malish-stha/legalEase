import { SignIn } from "@clerk/nextjs";

export default function SignInPage() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <SignIn />
    </div>
  );
}

// ── Premium split-panel layout (commented out) ─────────────────────────────
//
// "use client";
//
// import { SignIn } from "@clerk/nextjs";
// import { Scale, FileText, Shield, Zap } from "lucide-react";
// import Link from "next/link";
//
// const features = [
//   { icon: FileText, text: "Instant AI-powered document analysis" },
//   { icon: Shield, text: "Risk detection in plain language" },
//   { icon: Zap, text: "Real-time legal chatbot assistance" },
// ];
//
// export default function SignInPage() {
//   return (
//     <div className="min-h-screen flex" dir="ltr">
//
//       {/* ── Left branding panel */}
//       <div className="hidden lg:flex flex-col justify-between w-[480px] shrink-0 relative overflow-hidden bg-foreground text-background p-12">
//
//         {/* Ambient orbs */}
//         <div
//           className="pointer-events-none absolute -top-24 -left-24 w-80 h-80 rounded-full opacity-20"
//           style={{ background: "radial-gradient(circle, oklch(0.65 0.18 50) 0%, transparent 70%)" }}
//         />
//         <div
//           className="pointer-events-none absolute -bottom-16 -right-16 w-64 h-64 rounded-full opacity-15"
//           style={{ background: "radial-gradient(circle, oklch(0.55 0.16 45) 0%, transparent 70%)" }}
//         />
//
//         {/* Grid pattern */}
//         <div
//           className="pointer-events-none absolute inset-0 opacity-[0.04]"
//           style={{
//             backgroundImage:
//               "linear-gradient(white 1px, transparent 1px), linear-gradient(90deg, white 1px, transparent 1px)",
//             backgroundSize: "48px 48px",
//           }}
//         />
//
//         {/* Brand */}
//         <div className="relative">
//           <Link href="/" className="inline-flex items-center gap-2.5 group">
//             <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center shadow-lg">
//               <Scale className="w-4.5 h-4.5 text-primary-foreground" />
//             </div>
//             <span className="text-xl font-serif font-bold text-background">LegalEase</span>
//             <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-white/10 text-white/80 border border-white/15">
//               Nepal
//             </span>
//           </Link>
//         </div>
//
//         {/* Center copy */}
//         <div className="relative space-y-8">
//           <div className="space-y-4">
//             <h1 className="text-4xl font-serif font-bold text-background leading-tight">
//               Legal clarity,<br />
//               <span
//                 style={{
//                   background: "linear-gradient(90deg, oklch(0.75 0.14 55), oklch(0.85 0.10 70))",
//                   WebkitBackgroundClip: "text",
//                   WebkitTextFillColor: "transparent",
//                   backgroundClip: "text",
//                 }}
//               >
//                 simplified.
//               </span>
//             </h1>
//             <p className="text-background/60 text-base font-light leading-relaxed">
//               Analyze legal documents in minutes. Understand risks before signing.
//             </p>
//           </div>
//
//           <ul className="space-y-4">
//             {features.map(({ icon: Icon, text }, i) => (
//               <li key={i} className="flex items-center gap-3">
//                 <div className="w-8 h-8 rounded-lg bg-white/8 border border-white/10 flex items-center justify-center shrink-0">
//                   <Icon className="w-3.5 h-3.5 text-background/70" />
//                 </div>
//                 <span className="text-sm text-background/70">{text}</span>
//               </li>
//             ))}
//           </ul>
//         </div>
//
//         {/* Footer */}
//         <div className="relative">
//           <p className="text-xs text-background/30">
//             © {new Date().getFullYear()} LegalEase Nepal
//           </p>
//         </div>
//       </div>
//
//       {/* ── Right: Clerk widget */}
//       <div
//         className="flex-1 flex flex-col items-center justify-center min-h-screen relative overflow-hidden px-4"
//         style={{ backgroundColor: "#fafaf8" }}
//       >
//         <div className="pointer-events-none absolute top-0 right-0 w-96 h-96 opacity-10"
//           style={{ background: "radial-gradient(circle at top right, oklch(0.65 0.18 50) 0%, transparent 70%)" }}
//         />
//         <div className="pointer-events-none absolute bottom-0 left-0 w-72 h-72"
//           style={{ background: "radial-gradient(circle at bottom left, oklch(0.60 0.14 45 / 0.08) 0%, transparent 70%)" }}
//         />
//
//         {/* Mobile brand */}
//         <div className="lg:hidden mb-8 flex items-center gap-2">
//           <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
//             <Scale className="w-4 h-4 text-primary-foreground" />
//           </div>
//           <span className="text-lg font-serif font-bold" style={{ color: "#1a1a0e" }}>LegalEase Nepal</span>
//         </div>
//
//         <div className="relative w-full max-w-md">
//           <SignIn
//             appearance={{
//               variables: {
//                 colorBackground: "#ffffff",
//                 colorInputBackground: "#f5f4f0",
//                 colorText: "#1a1a0e",
//                 colorTextSecondary: "#6b6855",
//                 colorPrimary: "#b45309",
//                 colorInputText: "#1a1a0e",
//                 borderRadius: "0.75rem",
//                 fontFamily: "inherit",
//               },
//               elements: {
//                 card: "shadow-xl border border-stone-200",
//                 headerTitle: "font-serif text-[#1a1a0e]",
//                 headerSubtitle: "text-[#6b6855]",
//                 formButtonPrimary: "bg-[#b45309] hover:bg-[#92400e] text-white",
//                 footerActionLink: "text-[#b45309] hover:text-[#92400e]",
//               },
//             }}
//           />
//         </div>
//       </div>
//     </div>
//   );
// }
