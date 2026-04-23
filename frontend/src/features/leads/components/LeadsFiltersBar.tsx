import type { DateRange } from 'react-day-picker';
import { Calendar as CalendarIcon, Filter, Loader2, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { Input } from '@/components/ui/input';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { originOptions, stageOptions } from '../constants';
import type { AssignedUserOption, LeadAssignedFilter, LeadOriginFilter, LeadStageFilter } from '../types';

interface LeadsFiltersBarProps {
  search: string;
  origin: LeadOriginFilter;
  stage: LeadStageFilter;
  assignedTo: LeadAssignedFilter;
  assignedUsers: AssignedUserOption[];
  dateRange?: DateRange;
  dateRangeLabel: string;
  isDateRangeOpen: boolean;
  isFetching: boolean;
  onSearchChange: (value: string) => void;
  onOriginChange: (value: LeadOriginFilter) => void;
  onStageChange: (value: LeadStageFilter) => void;
  onAssignedToChange: (value: LeadAssignedFilter) => void;
  onDateRangeOpenChange: (open: boolean) => void;
  onDateRangeChange: (range?: DateRange) => void;
  onClearDateRange: () => void;
}

export function LeadsFiltersBar({
  search,
  origin,
  stage,
  assignedTo,
  assignedUsers,
  dateRange,
  dateRangeLabel,
  isDateRangeOpen,
  isFetching,
  onSearchChange,
  onOriginChange,
  onStageChange,
  onAssignedToChange,
  onDateRangeOpenChange,
  onDateRangeChange,
  onClearDateRange,
}: LeadsFiltersBarProps) {
  return (
    <div className="grid gap-3 md:grid-cols-5">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Buscar por nome, telefone ou email"
          className="pl-10 pr-10"
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
        />
        {isFetching ? (
          <Loader2 className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
        ) : null}
      </div>

      <div className="relative">
        <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Select value={origin} onValueChange={(value) => onOriginChange(value as LeadOriginFilter)}>
          <SelectTrigger className="pl-10">
            <SelectValue placeholder="Todas as origens" />
          </SelectTrigger>
          <SelectContent>
            {originOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="relative">
        <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Select value={stage} onValueChange={(value) => onStageChange(value as LeadStageFilter)}>
          <SelectTrigger className="pl-10">
            <SelectValue placeholder="Todos os estagios" />
          </SelectTrigger>
          <SelectContent>
            {stageOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="relative">
        <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Select value={assignedTo} onValueChange={(value) => onAssignedToChange(value as LeadAssignedFilter)}>
          <SelectTrigger className="pl-10">
            <SelectValue placeholder="Todos os responsaveis" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos os responsaveis</SelectItem>
            {assignedUsers.map((seller) => (
              <SelectItem key={seller.id} value={seller.id}>
                {seller.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Popover open={isDateRangeOpen} onOpenChange={onDateRangeOpenChange}>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            className={cn('h-10 w-full justify-start text-left font-normal', !dateRange?.from && 'text-muted-foreground')}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            <span className="truncate">{dateRangeLabel}</span>
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="range"
            selected={dateRange}
            defaultMonth={dateRange?.from}
            onSelect={onDateRangeChange}
            numberOfMonths={1}
          />
          {dateRange?.from ? (
            <div className="border-t p-2">
              <Button type="button" variant="ghost" size="sm" className="w-full justify-center" onClick={onClearDateRange}>
                Limpar periodo
              </Button>
            </div>
          ) : null}
        </PopoverContent>
      </Popover>
    </div>
  );
}
