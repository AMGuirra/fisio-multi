package com.fisiogestao.shared.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("status", status.value());
        model.put("title", titleFor(status));
        model.put("message", messageFor(status));
        model.put("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        return new ModelAndView("error/index", model, status);
    }

    @ResponseBody
    @RequestMapping(value = "/error")
    public ResponseEntity<Map<String, Object>> errorJson(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", messageFor(status));
        body.put("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            HttpStatus status = HttpStatus.resolve(code);
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String titleFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Pagina nao encontrada";
            case FORBIDDEN -> "Acesso negado";
            case UNAUTHORIZED -> "Login necessario";
            case TOO_MANY_REQUESTS -> "Muitas tentativas";
            default -> "Nao foi possivel concluir a solicitacao";
        };
    }

    private String messageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "O endereco acessado nao existe ou foi movido.";
            case FORBIDDEN -> "Voce nao tem permissao para acessar esta area.";
            case UNAUTHORIZED -> "Entre novamente para continuar usando o sistema.";
            case TOO_MANY_REQUESTS -> "Aguarde alguns instantes antes de tentar de novo.";
            default -> "Encontramos uma instabilidade ao processar esta pagina. Tente novamente em instantes.";
        };
    }
}
