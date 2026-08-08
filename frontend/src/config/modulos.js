import { LayoutDashboard, Boxes, Tags, FileText, Ticket, Users, Plug } from 'lucide-vue-next'

export const MODULOS = {
  ativos: {
    titulo: 'Gestão de Ativos',
    links: [
      { href: '/ativos/dashboard', label: 'Dashboard', iconComponent: LayoutDashboard, tip: 'Tela inicial que apresenta os principais indicadores e informações dos ativos, oferecendo uma visão geral do ambiente para acompanhamento da operação.' },
      { href: '/ativos', label: 'Ativos', iconComponent: Boxes, tip: 'Tela destinada ao gerenciamento dos ativos cadastrados no sistema, permitindo consultar, cadastrar, editar e excluir registros.' },
      { href: '/ativos/tipos', label: 'Tipos de Ativo', iconComponent: Tags, tip: 'Tela para cadastro e gerenciamento dos tipos de ativos e de seus respectivos campos, permitindo definir as informações que serão utilizadas em cada categoria de ativo.' },
      { href: '/ativos/relatorios', label: 'Relatórios', iconComponent: FileText, tip: 'Tela para consulta dos ativos cadastrados, com opção de selecionar dinamicamente as colunas exibidas e exportar os dados para análise externa.' }
    ]
  },
  chamados: {
    titulo: 'Gestão de Chamados',
    links: [
      { href: '/chamados', label: 'Chamados', iconComponent: Ticket }
    ]
  },
  usuarios: {
      titulo: 'Gestão de Usuários',
      links: [
        { href: '/usuarios', label: 'Usuários', iconComponent: Users }
      ]
    },
    configuracoes: {
      titulo: 'Configurações',
      links: [
        { href: '/configuracoes/integracao', label: 'Integração via API', iconComponent: Plug }
      ]
    }
  }