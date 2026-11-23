import { cn } from '@/lib/utils';
import { Check } from 'lucide-react';

export interface StepInfo {
  index: number;
  label: string;
}

interface OnboardingStepperProps {
  steps: StepInfo[];
  currentStep: number; // 0-based
}

export function OnboardingStepper({ steps, currentStep }: OnboardingStepperProps) {
  return (
    <nav aria-label="Progresso do cadastro" className="mb-6">
      {/* Screen-reader live region for step announcements */}
      <div aria-live="polite" className="sr-only">
        {steps[currentStep]?.label && `Etapa ${currentStep + 1} de ${steps.length}: ${steps[currentStep].label}`}
      </div>

      <ol className="flex items-center justify-between gap-1 md:gap-2">
        {steps.map((step, idx) => {
          const isCompleted = idx < currentStep;
          const isCurrent = idx === currentStep;
          const isFuture = idx > currentStep;

          return (
            <li key={step.index} className="flex flex-1 flex-col items-center">
              {/* Connector line before */}
              <div className="flex w-full items-center">
                {idx > 0 && (
                  <div
                    className={cn(
                      'h-0.5 flex-1 transition-colors',
                      idx <= currentStep ? 'bg-[hsl(var(--kanban-completed))]' : 'bg-muted',
                    )}
                  />
                )}

                {/* Step dot */}
                <button
                  type="button"
                  aria-current={isCurrent ? 'step' : undefined}
                  aria-label={`Etapa ${idx + 1}: ${step.label}${isCompleted ? ' (concluída)' : isCurrent ? ' (atual)' : ''}`}
                  disabled
                  className={cn(
                    'flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold transition-colors md:h-10 md:w-10',
                    isCompleted &&
                      'bg-[hsl(var(--kanban-completed))] text-white',
                    isCurrent &&
                      'bg-primary text-primary-foreground ring-4 ring-primary/10',
                    isFuture && 'bg-muted text-muted-foreground',
                  )}
                >
                  {isCompleted ? (
                    <Check className="h-4 w-4 md:h-5 md:w-5" />
                  ) : (
                    <span>{idx + 1}</span>
                  )}
                </button>

                {idx < steps.length - 1 && (
                  <div
                    className={cn(
                      'h-0.5 flex-1 transition-colors',
                      idx < currentStep ? 'bg-[hsl(var(--kanban-completed))]' : 'bg-muted',
                    )}
                  />
                )}
              </div>

              {/* Step label — hidden on mobile, visible md+ */}
              <span
                className={cn(
                  'mt-1 hidden text-xs font-normal md:block',
                  isCurrent ? 'text-primary font-semibold' : 'text-muted-foreground',
                )}
              >
                {step.label}
              </span>
              {/* Sr-only label for mobile accessibility */}
              <span className="sr-only md:hidden">{step.label}</span>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
