(function () {
  const MODULOS = {
    ativos: {
      titulo: "Gestão de Ativos",
      links: [
        { href: "/ativos/dashboard.html", label: "Dashboard" },
        { href: "/ativos/ativos.html", label: "Ativos" },
        { href: "/ativos/tipos.html", label: "Tipos de Ativo" },
        { href: "/ativos/relatorios.html", label: "Relatórios" }
      ]
    },
    chamados: {
      titulo: "Gestão de Chamados",
      links: [
        { href: "/chamados/chamados.html", label: "Chamados" }
      ]
    },
    usuarios: {
      titulo: "Gestão de Usuários",
      links: [
        { href: "/usuarios/usuarios.html", label: "Usuários" }
      ]
    }
  };

  const segmento = location.pathname.split("/")[1];
  const mod = MODULOS[segmento];
  if (!mod) return;

  const current = location.pathname;
  const nav = mod.links
    .map(l => `<a href="${l.href}" class="sidebar-link${l.href === current ? " active" : ""}">${l.label}</a>`)
    .join("");

  const aside = document.createElement("aside");
  aside.className = "sidebar";
  aside.innerHTML = `
    <div class="sidebar-title">${mod.titulo}</div>
    <nav class="sidebar-nav">${nav}</nav>
    <div class="sidebar-footer">
      <a href="/home.html" class="sidebar-link">← Voltar ao início</a>
    </div>`;

  document.body.insertAdjacentElement("afterbegin", aside);
})();