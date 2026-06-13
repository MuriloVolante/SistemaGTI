(function () {
  const u = JSON.parse(sessionStorage.getItem("gti_usuario") || "null");
  const isTI = u && u.tipoAcesso === "TI";

  const links = [
    { href: "dashboard.html", label: "Dashboard" },
    { href: "ativos.html", label: "Ativos" },
    { href: "tipos.html", label: "Tipos de Ativo" },
    { href: "relatorios.html", label: "Relatórios" },
    { href: "chamados.html", label: "Chamados", tiOnly: true }
  ].filter(l => !l.tiOnly || isTI);

  const current = location.pathname.split("/").pop() || "dashboard.html";

  const nav = links
    .map(l => `<a href="${l.href}" class="sidebar-link${l.href === current ? " active" : ""}">${l.label}</a>`)
    .join("");

  const aside = document.createElement("aside");
  aside.className = "sidebar";
  aside.innerHTML = `
    <div class="sidebar-title">Gestão de Ativos</div>
    <nav class="sidebar-nav">${nav}</nav>
    <div class="sidebar-footer">
      <a href="home.html" class="sidebar-link">← Voltar ao início</a>
    </div>`;

  document.body.insertAdjacentElement("afterbegin", aside);
})();