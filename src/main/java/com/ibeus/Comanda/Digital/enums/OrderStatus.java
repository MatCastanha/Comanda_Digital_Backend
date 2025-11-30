package com.ibeus.Comanda.Digital.enums;

public enum OrderStatus {
    DRAFT,           // 1. Rascunho/Carrinho (Novo estado inicial)
    RECEIVED,        // 2. Pedido chegou (Finalizado pelo cliente, esperando a cozinha)
    IN_PREPARATION,  // 3. Cozinha aceitou e está fazendo
    READY,           // 4. Pronto para retirada/entrega
    ON_THE_WAY,      // 5. A caminho do cliente
    DELIVERED        // 6. Finalizado
}