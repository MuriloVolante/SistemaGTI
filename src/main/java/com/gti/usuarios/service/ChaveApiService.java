package com.gti.usuarios.service;

import com.gti.usuarios.dto.ChaveApiCriadaDTO;
import com.gti.usuarios.dto.ChaveApiCriarDTO;
import com.gti.usuarios.dto.ChaveApiDTO;
import com.gti.usuarios.dto.ChaveApiDetalheDTO;
import com.gti.usuarios.dto.ChaveApiUsoDTO;
import com.gti.usuarios.model.ChaveApi;
import com.gti.usuarios.model.ChaveApiUso;
import com.gti.usuarios.model.EscopoIntegracao;
import com.gti.usuarios.repository.ChaveApiRepository;
import com.gti.usuarios.repository.ChaveApiUsoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ChaveApiService {

    private static final String PREFIXO_VALOR = "gti_";
    private static final int TAMANHO_PREFIXO = 12;
    private static final int BYTES_ALEATORIOS = 32;

    private static final Pattern IP_VALIDO = Pattern.compile(
            "^(\\d{1,3}\\.){3}\\d{1,3}(/\\d{1,2})?$|^[0-9a-fA-F:]+(/\\d{1,3})?$");

    private final ChaveApiRepository repo;
    private final ChaveApiUsoRepository usoRepo;
    private final SecureRandom random = new SecureRandom();

    public ChaveApiService(ChaveApiRepository repo, ChaveApiUsoRepository usoRepo) {
        this.repo = repo;
        this.usoRepo = usoRepo;
    }

    @Transactional(readOnly = true)
    public List<ChaveApiDTO> listar() {
        return repo.findAllByOrderByAtivaDescCriadoEmDesc()
                .stream().map(ChaveApiDTO::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChaveApi buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chave não encontrada."));
    }

    @Transactional
    public ChaveApiCriadaDTO criar(ChaveApiCriarDTO dto, Long criadoPorId, String criadoPorNome) {
        String nome = dto.nome() == null ? "" : dto.nome().trim();
        if (nome.isEmpty())
            throw new IllegalArgumentException("O nome da chave é obrigatório.");
        if (nome.length() > 120)
            throw new IllegalArgumentException("O nome da chave deve ter no máximo 120 caracteres.");
        if (repo.existsByNomeIgnoreCaseAndAtivaTrue(nome))
            throw new IllegalArgumentException("Já existe uma chave ativa com esse nome.");

        String escopos = normalizarEscopos(dto.escopos());
        String ips = normalizarIps(dto.ipsPermitidos());
        LocalDateTime expiraEm = calcularValidade(dto.diasValidade());

        String valor = gerarValor();

        ChaveApi c = new ChaveApi();
        c.setNome(nome);
        c.setPrefixo(valor.substring(0, TAMANHO_PREFIXO));
        c.setHashChave(hash(valor));
        c.setEscopos(escopos);
        c.setIpsPermitidos(ips);
        c.setExpiraEm(expiraEm);
        c.setAtiva(true);
        c.setCriadoPorId(criadoPorId);
        c.setCriadoPorNome(criadoPorNome);
        c.setCriadoEm(LocalDateTime.now());

        repo.save(c);
        return new ChaveApiCriadaDTO(ChaveApiDTO.from(c), valor);
    }

    @Transactional
    public ChaveApiDTO desligar(Long id, String desligadaPorNome) {
        ChaveApi c = buscar(id);
        if (!c.isAtiva())
            throw new IllegalArgumentException("Esta chave já está desligada.");
        c.setAtiva(false);
        c.setDesligadaEm(LocalDateTime.now());
        c.setDesligadaPorNome(desligadaPorNome);
        repo.save(c);
        return ChaveApiDTO.from(c);
    }

    @Transactional
    public ChaveApiCriadaDTO girar(Long id, Integer diasCarencia, Long autorId, String autorNome) {
        ChaveApi antiga = buscar(id);
        if (!antiga.isAtiva())
            throw new IllegalArgumentException("Só é possível girar uma chave ativa.");

        int carencia = (diasCarencia == null || diasCarencia < 0) ? 7 : Math.min(diasCarencia, 90);

        String nomeNova = antiga.getNome();
        antiga.setNome(antiga.getNome() + " (substituída em "
                + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        LocalDateTime limite = LocalDateTime.now().plusDays(carencia);
        if (antiga.getExpiraEm() == null || antiga.getExpiraEm().isAfter(limite))
            antiga.setExpiraEm(limite);
        repo.save(antiga);

        Integer diasValidade = antiga.getExpiraEm() == null ? null
                : (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), antiga.getExpiraEm().toLocalDate());

        ChaveApiCriarDTO dto = new ChaveApiCriarDTO(
                nomeNova,
                antiga.getEscoposLista(),
                diasValidade != null && diasValidade > 0 ? diasValidade : null,
                antiga.getIpsLista());

        ChaveApiCriadaDTO nova = criar(dto, autorId, autorNome);
        antiga.setSubstituidaPorId(nova.chave().id());
        repo.save(antiga);
        return nova;
    }

    @Transactional(readOnly = true)
    public ChaveApiDetalheDTO detalhar(Long id) {
        ChaveApi c = buscar(id);
        LocalDateTime desde = LocalDateTime.now().minusDays(30);

        List<ChaveApiDetalheDTO.PontoUso> serie = usoRepo.agruparPorDia(id, desde).stream()
                .map(l -> new ChaveApiDetalheDTO.PontoUso(
                        paraData(l[0]),
                        ((Number) l[1]).longValue(),
                        l[2] == null ? 0L : ((Number) l[2]).longValue()))
                .collect(Collectors.toList());

        List<ChaveApiUsoDTO> ultimas = usoRepo.findTop50ByChaveIdOrderByOcorridoEmDesc(id)
                .stream().map(ChaveApiUsoDTO::from).collect(Collectors.toList());

        return new ChaveApiDetalheDTO(
                ChaveApiDTO.from(c),
                usoRepo.countByChaveIdAndOcorridoEmAfter(id, desde),
                usoRepo.countByChaveIdAndSucessoFalseAndOcorridoEmAfter(id, desde),
                serie,
                ultimas);
    }

    @Transactional(readOnly = true)
    public Page<ChaveApiUsoDTO> tentativasNegadas(int pagina, int tamanho) {
        Pageable p = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 200));
        return usoRepo.findBySucessoFalseOrderByOcorridoEmDesc(p).map(ChaveApiUsoDTO::from);
    }

    private LocalDate paraData(Object valor) {
        if (valor instanceof java.sql.Date d) return d.toLocalDate();
        if (valor instanceof LocalDate d) return d;
        if (valor instanceof java.time.LocalDateTime dt) return dt.toLocalDate();
        return LocalDate.parse(String.valueOf(valor).substring(0, 10));
    }

    @Transactional(readOnly = true)
    public Optional<ChaveApi> buscarPorValor(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        return repo.findByHashChave(hash(valor));
    }

    public String prefixoDe(String valor) {
        if (valor == null) return null;
        return valor.length() >= TAMANHO_PREFIXO ? valor.substring(0, TAMANHO_PREFIXO) : valor;
    }

    public static String hash(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", e);
        }
    }

    private String gerarValor() {
        byte[] bytes = new byte[BYTES_ALEATORIOS];
        random.nextBytes(bytes);
        return PREFIXO_VALOR + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizarEscopos(List<String> escopos) {
        if (escopos == null || escopos.isEmpty())
            throw new IllegalArgumentException("Selecione pelo menos um conjunto de dados.");
        List<String> validos = escopos.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toUpperCase())
                .distinct()
                .toList();
        for (String e : validos) {
            try {
                EscopoIntegracao.valueOf(e);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Conjunto de dados inválido: " + e);
            }
        }
        if (validos.isEmpty())
            throw new IllegalArgumentException("Selecione pelo menos um conjunto de dados.");
        return String.join(",", validos);
    }

    private String normalizarIps(List<String> ips) {
        if (ips == null || ips.isEmpty()) return null;
        List<String> validos = ips.stream()
                .filter(i -> i != null && !i.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        for (String ip : validos) {
            if (!IP_VALIDO.matcher(ip).matches())
                throw new IllegalArgumentException("Endereço de rede inválido: " + ip);
        }
        return validos.isEmpty() ? null : String.join(",", validos);
    }

    private LocalDateTime calcularValidade(Integer dias) {
        if (dias == null || dias <= 0) return null;
        if (dias > 3650)
            throw new IllegalArgumentException("A validade máxima é de 3650 dias.");
        return LocalDateTime.now().plusDays(dias);
    }
}