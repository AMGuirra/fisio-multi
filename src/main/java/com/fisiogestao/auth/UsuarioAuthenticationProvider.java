package com.fisiogestao.auth;

import com.fisiogestao.empresa.EmpresaAcessoService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.MotivoAcessoNegado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Autentica usuários considerando a clínica:
 * <ul>
 *   <li>com código da clínica: procura o usuário (username ou e-mail) somente naquela empresa;</li>
 *   <li>sem código: tenta o SUPER_ADMIN e, em seguida, usuários de clínica cujo username/e-mail
 *       coincida; só autentica se exatamente uma credencial de clínica conferir com a senha.</li>
 * </ul>
 * Toda falha de credencial retorna a mesma mensagem genérica (sem enumeração de usuário ou clínica).
 * O status da empresa só é revelado depois que a senha confere.
 */
@Component
@RequiredArgsConstructor
public class UsuarioAuthenticationProvider implements AuthenticationProvider {

    static final String MENSAGEM_GENERICA = "Usuário ou senha inválidos.";

    /** Hash BCrypt descartável para equalizar o tempo de resposta quando nenhum candidato existe. */
    private volatile String dummyHash;

    private final UsuarioSistemaRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = InMemoryUserCatalog.normalizeUsernameForLookup(authentication.getName());
        String senha = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        String slug = authentication.getDetails() instanceof LoginDetails details ? details.getClinicaSlug() : null;

        if (login == null || senha.isEmpty()) {
            throw new BadCredentialsException(MENSAGEM_GENERICA);
        }

        List<UsuarioSistemaEntity> candidatos = buscarCandidatos(login, slug);
        if (candidatos.isEmpty()) {
            passwordEncoder.matches(senha, dummyHash());
            throw new BadCredentialsException(MENSAGEM_GENERICA);
        }

        UsuarioSistemaEntity autenticado = null;
        for (UsuarioSistemaEntity candidato : candidatos) {
            if (!passwordEncoder.matches(senha, candidato.getPasswordHash())) {
                continue;
            }
            if (autenticado == null) {
                autenticado = candidato;
            } else if (autenticado.getEmpresaId() != null) {
                // Mesmas credenciais em duas clínicas: é preciso informar o código da clínica.
                throw new BadCredentialsException(MENSAGEM_GENERICA);
            }
        }
        if (autenticado == null) {
            throw new BadCredentialsException(MENSAGEM_GENERICA);
        }

        UsuarioAutenticado principal = montarPrincipal(autenticado);
        UsernamePasswordAuthenticationToken resultado =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        resultado.setDetails(authentication.getDetails());
        return resultado;
    }

    /** Valida o vínculo usuário/empresa e o status de acesso da clínica. */
    public UsuarioAutenticado montarPrincipal(UsuarioSistemaEntity usuario) {
        if (usuario.getRole() == UserRole.SUPER_ADMIN) {
            if (usuario.getEmpresaId() != null) {
                throw new BadCredentialsException(MENSAGEM_GENERICA);
            }
            return UsuarioAutenticado.of(usuario, null);
        }
        if (usuario.getEmpresaId() == null) {
            throw new BadCredentialsException(MENSAGEM_GENERICA);
        }
        EmpresaEntity empresa = empresaRepository.findById(usuario.getEmpresaId())
                .orElseThrow(() -> new BadCredentialsException(MENSAGEM_GENERICA));
        Optional<MotivoAcessoNegado> negado = EmpresaAcessoService.avaliar(empresa, OffsetDateTime.now());
        if (negado.isPresent()) {
            throw new EmpresaAcessoNegadoException(negado.get());
        }
        return UsuarioAutenticado.of(usuario, empresa.getSlug());
    }

    private List<UsuarioSistemaEntity> buscarCandidatos(String login, String slug) {
        boolean porEmail = login.contains("@");
        if (slug != null) {
            return empresaRepository.findBySlug(slug)
                    .flatMap(empresa -> porEmail
                            ? usuarioRepository.findByEmpresaIdAndEmailIgnoreCase(empresa.getId(), login)
                            : usuarioRepository.findByEmpresaIdAndUsername(empresa.getId(), login))
                    .map(List::of)
                    .orElse(List.of());
        }
        List<UsuarioSistemaEntity> candidatos = new ArrayList<>();
        (porEmail
                ? usuarioRepository.findByEmpresaIdIsNullAndEmailIgnoreCase(login)
                : usuarioRepository.findByEmpresaIdIsNullAndUsername(login))
                .ifPresent(candidatos::add);
        candidatos.addAll(porEmail
                ? usuarioRepository.findByEmailIgnoreCaseAndEmpresaIdIsNotNull(login)
                : usuarioRepository.findByUsernameAndEmpresaIdIsNotNull(login));
        return candidatos;
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("senha-descartavel-" + System.nanoTime());
        }
        return dummyHash;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
