import { Helmet } from 'react-helmet-async';
import { HeroSection } from '@/components/landing/HeroSection';
import { SocialProofBar } from '@/components/landing/SocialProofBar';
import { ProblemSolutionSection } from '@/components/landing/ProblemSolutionSection';
import { FeaturesGrid } from '@/components/landing/FeaturesGrid';
import { FeatureHighlight } from '@/components/landing/FeatureHighlight';
import { IntegrationsSection } from '@/components/landing/IntegrationsSection';
import { PricingSection } from '@/components/landing/PricingSection';
import { TestimonialsSection } from '@/components/landing/TestimonialsSection';
import { CTASection } from '@/components/landing/CTASection';

const structuredData = {
  '@context': 'https://schema.org',
  '@type': 'SoftwareApplication',
  name: 'Astro LeadFlow',
  applicationCategory: 'BusinessApplication',
  operatingSystem: 'Web',
  offers: { '@type': 'Offer', price: '0', priceCurrency: 'BRL' },
  description:
    'CRM para concessionarias de veiculos com integracao a Webmotors, Mercado Livre e WhatsApp',
};

export default function LandingPage() {
  return (
    <>
      <Helmet>
        <title>Astro LeadFlow — CRM para Concessionarias de Veiculos</title>
        <meta
          name="description"
          content="Centralize leads de Webmotors, Mercado Livre e WhatsApp em um so lugar. Dashboard, Kanban e automacoes para vender mais."
        />
        <meta
          property="og:title"
          content="Astro LeadFlow — Sua concessionaria no piloto automatico"
        />
        <meta
          property="og:description"
          content="CRM inteligente para concessionarias. Integracao com marketplaces, WhatsApp automatico e pipeline visual de vendas."
        />
        <meta property="og:type" content="website" />
        <meta property="og:locale" content="pt_BR" />
        <meta name="twitter:card" content="summary_large_image" />
        <script type="application/ld+json">
          {JSON.stringify(structuredData)}
        </script>
      </Helmet>
      <HeroSection />
      <SocialProofBar />
      <ProblemSolutionSection />
      <FeaturesGrid />
      <FeatureHighlight />
      <IntegrationsSection />
      <PricingSection />
      <TestimonialsSection />
      <CTASection />
    </>
  );
}
