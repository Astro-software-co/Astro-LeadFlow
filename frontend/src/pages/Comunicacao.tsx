import { useState, useEffect, useRef } from 'react';
import { Helmet } from 'react-helmet-async';
import { useLeads } from '@/hooks/api/useLeads';
import { useChat } from '@/hooks/api/useChat';
import { ChatMessageDTO } from '@/types/api/chat';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Skeleton } from '@/components/ui/skeleton';
import { ExternalLink, MessageCircle, User, Phone, Loader2, Search } from 'lucide-react';
import { cn } from '@/lib/utils';

function StatusBadge({ status }: { status: ChatMessageDTO['status'] }) {
  const config = {
    SENT: { className: 'bg-slate-100 text-slate-600', label: 'Enviada' },
    DELIVERED: { className: 'bg-blue-100 text-blue-700', label: 'Entregue' },
    READ: { className: 'bg-emerald-100 text-emerald-700', label: 'Lida' },
    FAILED: { className: 'bg-red-100 text-red-700', label: 'Falhou' },
  }[status];
  return (
    <Badge variant="outline" className={cn('text-[10px] px-1.5 py-0 h-4', config.className)}>
      {config.label}
    </Badge>
  );
}

export default function Comunicacao() {
  const [selectedLeadId, setSelectedLeadId] = useState<string | undefined>(undefined);
  const [leadSearch, setLeadSearch] = useState('');
  const scrollAreaRef = useRef<HTMLDivElement>(null);

  const leadsQuery = useLeads({ size: 50 });
  const chatQuery = useChat(selectedLeadId);

  const leads = leadsQuery.data?.content ?? [];
  const filteredLeads = leadSearch.trim()
    ? leads.filter(
        (l) =>
          l.nome?.toLowerCase().includes(leadSearch.toLowerCase()) ||
          l.telefone?.toLowerCase().includes(leadSearch.toLowerCase()),
      )
    : leads;
  const selectedLead = leads.find((l) => l.id === selectedLeadId);

  // Auto-scroll to bottom when chat messages change
  useEffect(() => {
    if (scrollAreaRef.current) {
      const viewport = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]');
      if (viewport) viewport.scrollTop = viewport.scrollHeight;
    }
  }, [chatQuery.data]);

  function renderMessage(msg: ChatMessageDTO) {
    const isOutbound = msg.direction === 'OUTBOUND';
    const date = new Date(msg.sentAt);
    const timeStr = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

    return (
      <div key={msg.id} className={cn('flex flex-col', isOutbound ? 'items-end' : 'items-start')}>
        <div
          className={cn(
            'max-w-[75%] px-4 py-2',
            isOutbound
              ? 'bg-primary text-primary-foreground rounded-2xl rounded-br-sm'
              : 'bg-muted text-foreground rounded-2xl rounded-bl-sm',
          )}
        >
          <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
        </div>
        <div className="flex items-center gap-1 mt-1">
          <span className="text-xs text-muted-foreground">{timeStr}</span>
          {isOutbound && <StatusBadge status={msg.status} />}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Helmet>
        <title>Comunicacao — Astro LeadFlow</title>
      </Helmet>

      <div>
        <h1 className="text-2xl font-semibold text-foreground">Comunicacao</h1>
        <p className="text-sm text-muted-foreground">
          Selecione um lead para visualizar o historico de conversa.
        </p>
      </div>

      <div className="flex flex-col lg:flex-row gap-4">
        {/* LEFT PANEL: Lead Selector */}
        <div className="lg:w-80 shrink-0 rounded-xl border bg-card shadow-sm flex flex-col">
          {/* Search */}
          <div className="p-4 border-b border-border/50">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar leads..."
                value={leadSearch}
                onChange={(e) => setLeadSearch(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>

          {/* Lead list */}
          <ScrollArea className="flex-1 max-h-[600px]">
            {leadsQuery.isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
              </div>
            ) : filteredLeads.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-8">Nenhum lead encontrado.</p>
            ) : (
              <ul>
                {filteredLeads.map((lead) => (
                  <li key={lead.id}>
                    <button
                      onClick={() => setSelectedLeadId(lead.id)}
                      className={cn(
                        'w-full text-left px-4 py-3 flex items-center gap-3 hover:bg-muted/50 transition-colors min-h-[48px]',
                        selectedLeadId === lead.id && 'bg-primary/5 border-l-2 border-primary',
                      )}
                    >
                      <div className="flex h-9 w-9 items-center justify-center rounded-full bg-muted shrink-0">
                        <User className="h-4 w-4 text-muted-foreground" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-foreground truncate">{lead.nome || 'Sem nome'}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {lead.telefone} · {lead.origem}
                        </p>
                      </div>
                      {!lead.telefone && (
                        <Badge variant="outline" className="text-xs shrink-0">
                          Sem telefone
                        </Badge>
                      )}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </ScrollArea>
        </div>

        {/* RIGHT PANEL: Chat Area */}
        <div className="flex-1 flex flex-col rounded-xl border bg-card shadow-sm min-h-[400px] lg:min-h-[600px]">
          {!selectedLeadId ? (
            /* Empty state */
            <div className="flex-1 flex flex-col items-center justify-center gap-3 text-center p-8">
              <MessageCircle className="h-12 w-12 text-muted-foreground/30" />
              <div>
                <p className="text-lg font-semibold text-foreground">Selecione um lead</p>
                <p className="text-sm text-muted-foreground">
                  Escolha um lead na lista ao lado para ver o historico de conversa.
                </p>
              </div>
            </div>
          ) : (
            <>
              {/* Lead info bar */}
              <div className="p-4 border-b border-border/50 flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
                  <User className="h-4 w-4 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">{selectedLead?.nome || 'Sem nome'}</p>
                  <p className="text-xs text-muted-foreground flex items-center gap-1">
                    <Phone className="h-3 w-3" /> {selectedLead?.telefone || 'Sem telefone'}
                  </p>
                </div>
                {selectedLead?.telefone ? (
                  <a
                    href={`https://wa.me/${selectedLead.telefone.replace(/\D/g, '')}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="Abrir conversa no WhatsApp"
                  >
                    <Button variant="outline" size="sm" className="shrink-0 gap-1.5">
                      <ExternalLink className="h-3.5 w-3.5" />
                      Abrir no WhatsApp
                    </Button>
                  </a>
                ) : (
                  <Button variant="outline" size="sm" className="shrink-0 gap-1.5" disabled>
                    <ExternalLink className="h-3.5 w-3.5" />
                    Sem telefone
                  </Button>
                )}
              </div>

              {/* Chat messages */}
              <ScrollArea ref={scrollAreaRef} className="flex-1 p-4" aria-label="Historico de mensagens">
                {chatQuery.isLoading ? (
                  <div className="space-y-4">
                    <div className="mr-auto max-w-[75%]">
                      <Skeleton className="h-12 w-48 rounded-2xl rounded-bl-sm" />
                    </div>
                    <div className="ml-auto max-w-[75%]">
                      <Skeleton className="h-12 w-56 rounded-2xl rounded-br-sm" />
                    </div>
                    <div className="mr-auto max-w-[75%]">
                      <Skeleton className="h-12 w-40 rounded-2xl rounded-bl-sm" />
                    </div>
                  </div>
                ) : chatQuery.error ? (
                  <div className="flex items-center justify-center p-6">
                    <div className="rounded-xl border bg-card p-6 shadow-sm w-full max-w-md">
                      <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar as mensagens</h2>
                      <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
                      <Button className="mt-4" onClick={() => void chatQuery.refetch()}>
                        Tentar novamente
                      </Button>
                    </div>
                  </div>
                ) : chatQuery.data?.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-8 gap-2 text-center">
                    <MessageCircle className="h-8 w-8 text-muted-foreground/30" />
                    <p className="text-sm text-muted-foreground">
                      Nenhuma mensagem neste historico ainda.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">{chatQuery.data?.map(renderMessage)}</div>
                )}
              </ScrollArea>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
