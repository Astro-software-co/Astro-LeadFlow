import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

interface LeadArchiveConfirmDialogProps {
  open: boolean;
  leadName?: string;
  isArchiving: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
}

export function LeadArchiveConfirmDialog({
  open,
  leadName,
  isArchiving,
  onOpenChange,
  onConfirm,
}: LeadArchiveConfirmDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Arquivar lead</AlertDialogTitle>
          <AlertDialogDescription>
            {leadName
              ? `Tem certeza que deseja arquivar o lead ${leadName}?`
              : 'Tem certeza que deseja arquivar este lead?'}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isArchiving}>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            disabled={isArchiving}
            onClick={(event) => {
              event.preventDefault();
              onConfirm();
            }}
          >
            {isArchiving ? 'Arquivando...' : 'Arquivar'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
