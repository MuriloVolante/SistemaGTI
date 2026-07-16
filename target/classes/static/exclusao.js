const EXCLUSAO_LABELS = {
    usuarios:              ['usuário', 'usuários'],
    tipos:                 ['tipo de ativo', 'tipos de ativo'],
    campos:                ['campo dinâmico', 'campos dinâmicos'],
    ativos:                ['ativo', 'ativos'],
    camposPreenchidos:     ['valor de campo preenchido', 'valores de campos preenchidos'],
    anexos:                ['anexo', 'anexos'],
    historico:             ['registro de histórico', 'registros de histórico'],
    manutencoes:           ['manutenção', 'manutenções'],
    chamados:              ['chamado', 'chamados'],
    mensagens:             ['mensagem de chamado', 'mensagens de chamado']
};

const EXCLUSAO_DESVINCULOS = {
    chamadosDesvinculados: ['chamado ficará sem técnico', 'chamados ficarão sem técnico'],
    ativosDesvinculados:   ['ativo ficará sem responsável', 'ativos ficarão sem responsável']
};

function exclusaoLinha(mapa, chave, valor) {
    const par = mapa[chave];
    if (!par) return null;
    return `<li><strong>${valor}</strong> ${valor === 1 ? par[0] : par[1]}</li>`;
}

function montarOverlayExclusao() {
    if (document.getElementById('overlay-exclusao')) return;
    const div = document.createElement('div');
    div.className = 'overlay';
    div.id = 'overlay-exclusao';
    div.innerHTML = `
        <div class="modal modal-exclusao">
            <div class="modal-title" id="exc-titulo">Excluir permanentemente</div>
            <div class="exc-alerta">
                Esta exclusão é <strong>permanente</strong> e não pode ser desfeita.
                Os registros abaixo serão apagados do banco de dados.
            </div>
            <div id="exc-carregando" class="exc-carregando">Calculando impacto...</div>
            <div id="exc-corpo" class="hidden">
                <div class="exc-secao-titulo">Será excluído</div>
                <ul class="exc-lista" id="exc-lista"></ul>
                <div id="exc-bloco-desvinculo" class="hidden">
                    <div class="exc-secao-titulo">Será desvinculado (não excluído)</div>
                    <ul class="exc-lista exc-lista-neutra" id="exc-lista-desvinculo"></ul>
                </div>
                <div class="field exc-campo">
                    <label>Digite <span id="exc-palavra" class="exc-palavra"></span> para confirmar</label>
                    <input type="text" id="exc-input" autocomplete="off">
                    <div class="erro-msg" id="exc-erro"></div>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn" id="exc-cancelar">Cancelar</button>
                <button class="btn btn-danger" id="exc-confirmar" disabled>Excluir permanentemente</button>
            </div>
        </div>`;
    document.body.appendChild(div);
    document.getElementById('exc-cancelar').onclick = fecharExclusao;
    div.onclick = e => { if (e.target === div) fecharExclusao(); };
}

function fecharExclusao() {
    const o = document.getElementById('overlay-exclusao');
    if (o) o.classList.remove('open');
}

async function abrirModalExclusao({ titulo, impactoUrl, deleteUrl, palavraConfirmacao, onSucesso }) {
    montarOverlayExclusao();

    const overlay    = document.getElementById('overlay-exclusao');
    const carregando = document.getElementById('exc-carregando');
    const corpo      = document.getElementById('exc-corpo');
    const input      = document.getElementById('exc-input');
    const btn        = document.getElementById('exc-confirmar');

    document.getElementById('exc-titulo').textContent = titulo;
        document.getElementById('exc-palavra').textContent = palavraConfirmacao;
        input.value = '';
        document.getElementById('exc-erro').classList.remove('show');
        input.classList.remove('exc-input-erro');
        btn.disabled = true;
    btn.textContent = 'Excluir permanentemente';
    corpo.classList.add('hidden');
    carregando.classList.remove('hidden');
    overlay.classList.add('open');

    let impacto;
    try {
        const res = await apiFetch(impactoUrl);
        if (!res.ok) {
            const err = await res.json();
            toast(err.erro || 'Erro ao calcular impacto.');
            fecharExclusao();
            return;
        }
        impacto = await res.json();
    } catch (e) {
        toast('Erro ao calcular impacto.');
        fecharExclusao();
        return;
    }

    const itens = [];
    const desvinculos = [];
    for (const [k, v] of Object.entries(impacto)) {
        if (!v) continue;
        const linha = exclusaoLinha(EXCLUSAO_LABELS, k, v);
        if (linha) { itens.push(linha); continue; }
        const desv = exclusaoLinha(EXCLUSAO_DESVINCULOS, k, v);
        if (desv) desvinculos.push(desv);
    }

    document.getElementById('exc-lista').innerHTML = itens.join('');
    const blocoDesv = document.getElementById('exc-bloco-desvinculo');
    if (desvinculos.length) {
        document.getElementById('exc-lista-desvinculo').innerHTML = desvinculos.join('');
        blocoDesv.classList.remove('hidden');
    } else {
        blocoDesv.classList.add('hidden');
    }

    carregando.classList.add('hidden');
    corpo.classList.remove('hidden');
    input.focus();

    const erro = document.getElementById('exc-erro');
        input.oninput = () => {
            const digitado = input.value.trim();
            const confere  = digitado === palavraConfirmacao;
            btn.disabled = !confere;

            if (confere || digitado === '') {
                        erro.classList.remove('show');
                        input.classList.remove('exc-input-erro');
            } else {
                erro.textContent = `O texto não confere com "${palavraConfirmacao}". A exclusão só é liberada com o texto exato.`;
                erro.classList.add('show');
                input.classList.add('exc-input-erro');
            }
        };

    btn.onclick = async () => {
        btn.disabled = true;
        btn.textContent = 'Excluindo...';
        try {
            const res = await apiFetch(deleteUrl, { method: 'DELETE' });
            if (!res.ok) {
                const err = await res.json();
                toast(err.erro || 'Erro ao excluir.');
                btn.disabled = false;
                btn.textContent = 'Excluir permanentemente';
                return;
            }
            fecharExclusao();
            toast('Excluído permanentemente.');
            if (onSucesso) onSucesso();
        } catch (e) {
            toast('Erro ao excluir.');
            btn.disabled = false;
            btn.textContent = 'Excluir permanentemente';
        }
    };
}