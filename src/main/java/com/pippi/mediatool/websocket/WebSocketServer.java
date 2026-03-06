package com.pippi.mediatool.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: hong
 * @CreateTime: 2026-02-23
 * @Description: 提供视频处理任务的WebSocket通信服务，支持实时进度推送
 * @Version: 1.0
 */
@Slf4j
@ServerEndpoint("/ws/media-tool/{taskId}")
@Component
public class WebSocketServer {

    // 保存所有活跃的WebSocket会话连接
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * WebSocket连接建立时自动调用
     * 将新建立的会话存入sessionMap中
     *
     * @param session 当前建立的WebSocket会话
     * @param taskId  路径参数，任务唯一标识符
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("taskId") String taskId) {
        sessionMap.put(taskId, session);
        log.info("WebSocket连接打开，任务ID: {}", taskId);
    }

    /**
     * WebSocket连接关闭时自动调用
     * 从sessionMap中移除对应的会话
     *
     * @param taskId 路径参数，任务唯一标识符
     */
    @OnClose
    public void onClose(@PathParam("taskId") String taskId) {
        sessionMap.remove(taskId);
        log.info("WebSocket连接关闭，任务ID: {}", taskId);
    }

    /**
     * 接收到客户端消息时自动调用
     *
     * @param message 客户端发送的消息内容
     * @param taskId  路径参数，任务唯一标识符
     */
    @OnMessage
    public void onMessage(String message, @PathParam("taskId") String taskId) {
        log.info("收到消息，任务ID: {}，消息: {}", taskId, message);
        // 可根据业务需求处理客户端消息，如暂停、取消下载等
    }

    /**
     * WebSocket发生错误时自动调用
     *
     * @param session 发生错误的会话
     * @param error   错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误", error);
    }

    /**
     * 主动推送下载进度到客户端
     * 供业务层调用，实时推送任务处理进度
     *
     * @param taskId     任务唯一标识符
     * @param percentage 下载进度百分比（0-100）
     */
    public static void sendProgress(String taskId, int percentage) {
        Session session = sessionMap.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(String.valueOf(percentage));
            } catch (IOException e) {
                log.error("发送进度失败，任务ID: {}", taskId, e);
                sessionMap.remove(taskId);
            }
        } else {
            log.warn("WebSocket会话不存在或已关闭，任务ID: {}", taskId);
        }
    }

    /**
     * 获取当前活跃的连接数
     *
     * @return 当前活跃的WebSocket连接数量
     */
    public static int getActiveConnectionCount() {
        return sessionMap.size();
    }

    /**
     * 检查指定任务是否在线
     *
     * @param taskId 任务ID
     * @return true: 在线，false: 不在线
     */
    public static boolean isOnline(String taskId) {
        Session session = sessionMap.get(taskId);
        return session != null && session.isOpen();
    }
}