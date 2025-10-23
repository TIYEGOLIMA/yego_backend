package com.yego.backend.service.yego_asistencia.impl;

import com.yego.backend.service.yego_asistencia.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {
    
    private final Random random = new Random();
    
    // Mensajes motivacionales por tipo
    private final List<String> generalMessages = Arrays.asList(
        "¡Excelente trabajo en Yego!",
        "¡Sigue así, eres parte del equipo ganador!",
        "¡Tu dedicación hace la diferencia!",
        "¡Gracias por tu compromiso con Yego!",
        "¡Eres un ejemplo a seguir!",
        "¡Tu esfuerzo se nota, sigue adelante!",
        "¡Juntos hacemos que Yego sea mejor!",
        "¡Tu puntualidad es admirable!",
        "¡Gracias por ser parte del equipo Yego!",
        "¡Tu profesionalismo es inspirador!"
    );
    
    private final List<String> entryMessages = Arrays.asList(
        "¡Buenos días! Que tengas un excelente día de trabajo",
        "¡Bienvenido! Hoy será un gran día en Yego",
        "¡Excelente puntualidad! Que tengas un día productivo",
        "¡Buenos días! Tu dedicación es admirable",
        "¡Que tengas un día lleno de éxitos!",
        "¡Bienvenido al equipo! Hoy será un gran día",
        "¡Tu puntualidad es un ejemplo para todos!",
        "¡Que tengas un día lleno de logros!",
        "¡Buenos días! Tu compromiso se nota",
        "¡Excelente inicio de día!"
    );
    
    private final List<String> exitBreakMessages = Arrays.asList(
        "¡Que disfrutes tu refrigerio!",
        "¡Disfruta tu descanso, te lo mereces!",
        "¡Que tengas un buen refrigerio!",
        "¡Disfruta tu tiempo de descanso!",
        "¡Que tengas un refrigerio delicioso!",
        "¡Disfruta tu pausa, regresa con energía!",
        "¡Que tengas un excelente refrigerio!",
        "¡Disfruta tu descanso merecido!",
        "¡Que tengas un buen tiempo de pausa!",
        "¡Disfruta tu refrigerio, regresa renovado!"
    );
    
    private final List<String> returnBreakMessages = Arrays.asList(
        "¡Bienvenido de vuelta! Que tengas una excelente tarde",
        "¡Esperamos que hayas disfrutado tu refrigerio!",
        "¡Bienvenido de regreso! Que tengas una gran tarde",
        "¡Esperamos que hayas descansado bien!",
        "¡Bienvenido de vuelta al trabajo!",
        "¡Que tengas una excelente tarde de trabajo!",
        "¡Bienvenido de regreso! Que tengas una tarde productiva",
        "¡Esperamos que hayas disfrutado tu pausa!",
        "¡Bienvenido de vuelta! Que tengas una gran tarde",
        "¡Que tengas una excelente tarde de trabajo!"
    );
    
    private final List<String> exitMessages = Arrays.asList(
        "¡Excelente día de trabajo! Que descanses bien",
        "¡Gracias por tu dedicación hoy! Que tengas una buena noche",
        "¡Felicitaciones por tu trabajo de hoy! Que descanses",
        "¡Excelente jornada! Que tengas una buena noche",
        "¡Gracias por tu esfuerzo! Que descanses bien",
        "¡Felicitaciones por tu día de trabajo! Que tengas una buena noche",
        "¡Excelente trabajo hoy! Que descanses bien",
        "¡Gracias por tu dedicación! Que tengas una buena noche",
        "¡Felicitaciones por tu jornada! Que descanses",
        "¡Excelente día! Que tengas una buena noche"
    );
    
    // ===== MÉTODOS PRINCIPALES =====
    
    @Override
    public String getRandomMessage(String messageType) {
        log.info("🎲 [MessageService] Obteniendo mensaje aleatorio para tipo: {}", messageType);
        
        switch (messageType.toLowerCase()) {
            case "entry":
            case "entrada":
                return getRandomMessage(entryMessages);
            case "exit":
            case "salida":
                return getRandomMessage(exitMessages);
            case "exit_break":
            case "salida_refrigerio":
                return getRandomMessage(exitBreakMessages);
            case "return_break":
            case "regreso_refrigerio":
                return getRandomMessage(returnBreakMessages);
            case "general":
            default:
                return getRandomMessage(generalMessages);
        }
    }
    
    @Override
    public String getRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "No hay mensajes disponibles.";
        }
        return messages.get(random.nextInt(messages.size()));
    }
    
    // ===== MÉTODOS POR TIPO DE MARCACIÓN =====
    
    @Override
    public String getEntryMessage() {
        return getRandomMessage(entryMessages);
    }
    
    @Override
    public String getExitBreakMessage() {
        return getRandomMessage(exitBreakMessages);
    }
    
    @Override
    public String getReturnBreakMessage() {
        return getRandomMessage(returnBreakMessages);
    }
    
    @Override
    public String getExitMessage() {
        return getRandomMessage(exitMessages);
    }
    
    @Override
    public String getGeneralMessage() {
        return getRandomMessage(generalMessages);
    }
    
    // ===== MÉTODOS DE UTILIDAD =====
    
    @Override
    public boolean isValidMessageType(String messageType) {
        return Arrays.asList("entry", "entrada", "exit", "salida", "exit_break", "salida_refrigerio", "return_break", "regreso_refrigerio", "general").contains(messageType.toLowerCase());
    }
    
    @Override
    public String getSuccessMessage(String actionType) {
        log.info("✅ [MessageService] Generando mensaje de éxito para acción: {}", actionType);
        return "¡" + actionType + " realizado exitosamente! 🎉";
    }
}