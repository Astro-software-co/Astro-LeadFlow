import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Car, Eye, EyeOff, Mail, Lock, User, Phone } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

export default function Login() {
  const navigate = useNavigate();
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState({ nome: '', email: '', telefone: '', senha: '', confirmarSenha: '' });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isLogin && form.senha !== form.confirmarSenha) {
      toast.error('As senhas não coincidem');
      return;
    }
    toast.success(isLogin ? 'Login realizado com sucesso!' : 'Conta criada com sucesso!');
    navigate('/');
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <div className="w-full max-w-md space-y-8">
        {/* Logo */}
        <div className="flex flex-col items-center gap-3">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary">
            <Car className="h-8 w-8 text-primary-foreground" />
          </div>
          <div className="text-center">
            <h1 className="text-2xl font-bold text-foreground">V3 Motors</h1>
            <p className="text-sm text-muted-foreground">CRM Automotivo</p>
          </div>
        </div>

        {/* Card */}
        <div className="rounded-2xl border border-border bg-card p-8 shadow-lg">
          {/* Tabs */}
          <div className="mb-6 flex rounded-lg bg-muted p-1">
            <button
              onClick={() => setIsLogin(true)}
              className={cn(
                'flex-1 rounded-md py-2 text-sm font-medium transition-all',
                isLogin ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
              )}
            >
              Entrar
            </button>
            <button
              onClick={() => setIsLogin(false)}
              className={cn(
                'flex-1 rounded-md py-2 text-sm font-medium transition-all',
                !isLogin ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
              )}
            >
              Cadastrar
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {!isLogin && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="nome">Nome Completo</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="nome"
                      placeholder="Seu nome completo"
                      value={form.nome}
                      onChange={(e) => setForm({ ...form, nome: e.target.value })}
                      className="pl-10"
                      required
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="telefone">Telefone</Label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="telefone"
                      placeholder="(11) 99999-9999"
                      value={form.telefone}
                      onChange={(e) => setForm({ ...form, telefone: e.target.value })}
                      className="pl-10"
                    />
                  </div>
                </div>
              </>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder="seu@email.com"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  className="pl-10"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="senha">Senha</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="senha"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  value={form.senha}
                  onChange={(e) => setForm({ ...form, senha: e.target.value })}
                  className="pl-10 pr-10"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {!isLogin && (
              <div className="space-y-2">
                <Label htmlFor="confirmarSenha">Confirmar Senha</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="confirmarSenha"
                    type="password"
                    placeholder="••••••••"
                    value={form.confirmarSenha}
                    onChange={(e) => setForm({ ...form, confirmarSenha: e.target.value })}
                    className="pl-10"
                    required
                  />
                </div>
              </div>
            )}

            {isLogin && (
              <div className="flex justify-end">
                <button type="button" className="text-xs text-primary hover:underline">
                  Esqueceu a senha?
                </button>
              </div>
            )}

            <Button type="submit" className="w-full">
              {isLogin ? 'Entrar' : 'Criar Conta'}
            </Button>
          </form>
        </div>

        <p className="text-center text-xs text-muted-foreground">
          © 2026 V3 Motors. Todos os direitos reservados.
        </p>
      </div>
    </div>
  );
}
