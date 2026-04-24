import { motion } from 'framer-motion';
import { CheckCircle2 } from 'lucide-react';
import { useReducedMotion } from 'framer-motion';

export function OnboardingSuccess() {
  const prefersReduced = useReducedMotion();

  const variants = prefersReduced
    ? { initial: {}, animate: {} }
    : {
        initial: { opacity: 0, scale: 0.96 },
        animate: { opacity: 1, scale: 1 },
      };

  const transition = prefersReduced ? { duration: 0 } : { duration: 0.18 };

  return (
    <div aria-live="polite" className="flex flex-col items-center py-12 text-center">
      <motion.div
        initial={variants.initial}
        animate={variants.animate}
        transition={transition}
        className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-[hsl(var(--kanban-completed)/0.1)]"
      >
        <CheckCircle2 className="h-10 w-10 text-[hsl(var(--kanban-completed))]" />
      </motion.div>

      <h1 className="mb-3 text-3xl font-semibold leading-tight">
        Cadastro enviado para análise
      </h1>

      <p className="max-w-sm text-sm leading-relaxed text-muted-foreground">
        Sua solicitação foi enviada e está em análise. Entraremos em contato em breve.
      </p>
    </div>
  );
}
