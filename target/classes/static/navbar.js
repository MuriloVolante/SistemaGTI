(function () {
  const MODULOS = {
      ativos: {
        titulo: "Gestão de Ativos",
        links: [
          { href: "/ativos/dashboard.html", label: "Dashboard", icon: "layout-dashboard", tip: "Tela inicial que apresenta os principais indicadores e informações dos ativos, oferecendo uma visão geral do ambiente para acompanhamento da operação." },
          { href: "/ativos/ativos.html", label: "Ativos", icon: "boxes", tip: "Tela destinada ao gerenciamento dos ativos cadastrados no sistema, permitindo consultar, cadastrar, editar e excluir registros." },
          { href: "/ativos/tipos.html", label: "Tipos de Ativo", icon: "tags", tip: "Tela para cadastro e gerenciamento dos tipos de ativos e de seus respectivos campos, permitindo definir as informações que serão utilizadas em cada categoria de ativo." },
          { href: "/ativos/relatorios.html", label: "Relatórios", icon: "file-text", tip: "Tela para consulta dos ativos cadastrados, com opção de selecionar dinamicamente as colunas exibidas e exportar os dados para análise externa." }
        ]
      },
      chamados: {
        titulo: "Gestão de Chamados",
        links: [
          { href: "/chamados/chamados.html", label: "Chamados", icon: "ticket" }
        ]
      },
      usuarios: {
              titulo: "Gestão de Usuários",
              links: [
                { href: "/usuarios/usuarios.html", label: "Usuários", icon: "users" }
              ]
            },
            configuracoes: {
              titulo: "Configurações",
              links: [
                { href: "/configuracoes/integracao.html", label: "Integração API", icon: "plug", tip: "Tela para consulta dos endpoints de integração externa (somente leitura), utilizados por sistemas de terceiros para ler dados do GTI mediante API key." }
              ]
            }
          };

  const segmento = location.pathname.split("/")[1];
  const mod = MODULOS[segmento];
  if (!mod) return;

  const current = location.pathname;
    const nav = mod.links
          .map(l => `<a href="${l.href}" class="sidebar-link${l.href === current ? " active" : ""}"><i data-lucide="${l.icon}"></i><span class="sidebar-link-label">${l.label}</span>${l.tip ? `<span class="info-icon info-icon-side" tabindex="0" onclick="event.preventDefault();event.stopPropagation();">i<span class="info-tip info-tip-side">${l.tip}</span></span>` : ""}</a>`)
          .join("");

    function lerTipoAcesso() {
      try {
        const token = sessionStorage.getItem("gti_token");
        if (!token) return null;
        const claims = JSON.parse(atob(token.split(".")[1]));
        return claims.tipoAcesso || null;
      } catch (e) {
        return null;
      }
    }

    const ehTI = lerTipoAcesso() === "TI";
      const linkConfig = (ehTI && segmento !== "configuracoes")
        ? `<a href="/configuracoes/integracao.html" class="sidebar-link"><i data-lucide="settings"></i><span class="sidebar-link-label">Configurações</span></a>`
        : "";

      const aside = document.createElement("aside");
      aside.className = "sidebar";
      aside.innerHTML = `
          <div class="sidebar-head">
            <div class="sidebar-title">${mod.titulo}</div>
            <button class="sidebar-toggle" type="button" aria-label="Expandir menu"><i data-lucide="chevron-right"></i></button>
          </div>
          <nav class="sidebar-nav">${nav}</nav>
          <div class="sidebar-footer">
            ${linkConfig}
            <a href="/home.html" class="sidebar-link"><i data-lucide="arrow-left"></i><span class="sidebar-link-label">Voltar ao início</span></a>
          </div>`;
        aside.classList.add("collapsed");

  document.body.insertAdjacentElement("afterbegin", aside);

    const toggle = aside.querySelector(".sidebar-toggle");
    toggle.addEventListener("click", () => {
      const colapsada = aside.classList.toggle("collapsed");
      toggle.setAttribute("aria-label", colapsada ? "Expandir menu" : "Recolher menu");
      toggle.innerHTML = `<i data-lucide="${colapsada ? "chevron-right" : "chevron-left"}"></i>`;
      if (window.lucide) window.lucide.createIcons();
    });

  if (window.lucide) {
      window.lucide.createIcons();
    } else {
      const s = document.createElement("script");
      s.src = "https://unpkg.com/lucide@latest";
      s.onload = () => window.lucide.createIcons();
      document.head.appendChild(s);
    }

    aside.querySelectorAll(".info-icon-side").forEach(ic => {
      const tip = ic.querySelector(".info-tip-side");
      document.body.appendChild(tip);
      const mostrar = () => {
        tip.classList.add("show");
        const r = ic.getBoundingClientRect();
        tip.style.left = (r.right + 8) + "px";
        tip.style.top = (r.top + r.height / 2 - tip.offsetHeight / 2) + "px";
      };
      const ocultar = () => tip.classList.remove("show");
      ic.addEventListener("mouseenter", mostrar);
      ic.addEventListener("mouseleave", ocultar);
      ic.addEventListener("focus", mostrar);
      ic.addEventListener("blur", ocultar);
    });
  })();