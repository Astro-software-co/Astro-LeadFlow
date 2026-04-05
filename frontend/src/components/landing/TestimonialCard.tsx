import { Avatar, AvatarFallback } from '@/components/ui/avatar';

interface TestimonialCardProps {
  quote: string;
  name: string;
  role: string;
  company: string;
  initials: string;
}

export function TestimonialCard({ quote, name, role, company, initials }: TestimonialCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col hover:shadow-md transition-shadow duration-200">
      <span className="text-primary text-5xl font-bold leading-none mb-2" aria-hidden="true">
        &ldquo;
      </span>
      <p className="text-slate-700 text-base leading-relaxed flex-1 italic">{quote}</p>
      <div className="flex items-center gap-3 mt-6 border-t border-slate-100 pt-4">
        <Avatar>
          <AvatarFallback className="bg-primary text-white text-sm font-bold">{initials}</AvatarFallback>
        </Avatar>
        <div>
          <p className="text-sm font-bold text-slate-900">{name}</p>
          <p className="text-xs text-slate-500">
            {role}, {company}
          </p>
        </div>
      </div>
    </div>
  );
}
