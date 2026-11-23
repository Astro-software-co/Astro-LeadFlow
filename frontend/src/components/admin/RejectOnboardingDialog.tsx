import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';

const rejectSchema = z.object({
  reason: z.string().min(10, 'O motivo deve ter pelo menos 10 caracteres'),
});

type RejectFormValues = z.infer<typeof rejectSchema>;

interface RejectOnboardingDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (reason: string) => Promise<void>;
  isSubmitting: boolean;
}

export function RejectOnboardingDialog({
  open,
  onOpenChange,
  onConfirm,
  isSubmitting,
}: RejectOnboardingDialogProps) {
  const form = useForm<RejectFormValues>({
    resolver: zodResolver(rejectSchema),
    defaultValues: { reason: '' },
  });

  async function handleSubmit(values: RejectFormValues) {
    await onConfirm(values.reason);
    form.reset();
    onOpenChange(false);
  }

  function handleCancel() {
    form.reset();
    onOpenChange(false);
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Rejeitar cadastro</AlertDialogTitle>
          <AlertDialogDescription>
            Informe o motivo da rejeição. O responsável poderá ser notificado.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Motivo da rejeicao</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Descreva o motivo da rejeição (mínimo 10 caracteres)"
                      className="resize-none"
                      rows={4}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <AlertDialogFooter>
              <AlertDialogCancel onClick={handleCancel} disabled={isSubmitting}>
                Cancelar
              </AlertDialogCancel>
              <Button
                type="submit"
                variant="destructive"
                disabled={isSubmitting}
                aria-label="Rejeitar cadastro"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Rejeitando...
                  </>
                ) : (
                  'Rejeitar cadastro'
                )}
              </Button>
            </AlertDialogFooter>
          </form>
        </Form>
      </AlertDialogContent>
    </AlertDialog>
  );
}
