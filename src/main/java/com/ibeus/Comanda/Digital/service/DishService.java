package com.ibeus.Comanda.Digital.service;

import com.ibeus.Comanda.Digital.dto.DishDTO;
import com.ibeus.Comanda.Digital.model.Dish;
import com.ibeus.Comanda.Digital.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class DishService {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private StorageService storageService; // Injeta o serviço que sabe salvar arquivos

    // --- Buscas ---

    public List<Dish> findAll() {
        return dishRepository.findAll();
    }

    public Dish findById(Long id) {
        return dishRepository.findById(id)
                // Se não achar, lança erro 404 (Not Found) em vez de erro genérico
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prato não encontrado: " + id));
    }

    public List<Dish> findFavorites(){
        return dishRepository.findByFavoriteTrue();
    }

    public List<Dish> findByName(String name) {
        return dishRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Dish> findByCategory(String category) {
        List<Dish> dishes = dishRepository.findByCategoryIgnoreCase(category);
        if (dishes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum prato nesta categoria: " + category);
        }
        return dishes;
    }

    @Transactional
    public Dish toggleFavorite(Long id) {
        Dish existingDish = findById(id);
        
        // Inverte o valor booleano atual
        existingDish.setFavorite(!existingDish.isFavorite());
        
        return dishRepository.save(existingDish);
    }

    // --- Criação Unificada (Lógica Principal) ---

    public Dish create(DishDTO dishDTO, MultipartFile file) {
        try {
            // 1. Verifica se o usuário enviou uma imagem
            if (file != null && !file.isEmpty()) {
                // Se enviou, chama o StorageService para salvar no disco
                String imageUrl = storageService.store(file);
                // Atualiza o DTO com o caminho da nova imagem
                dishDTO.setUrlImage(imageUrl);
            }
            // Se file for null, ele mantém a URL que talvez já tenha vindo no DTO (ou fica null)

            // 2. Converte DTO -> Entity e salva no banco
            return dishRepository.save(dishDTO.toModel());

        } catch (Exception e) {
            // Captura erros e devolve um 400 Bad Request
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao processar dados do prato", e);
        }
    }

    // --- Atualização ---

    // --- Atualização Unificada (Novo Método) 🔄 ---
    /**
     * Atualiza o prato e a imagem opcionalmente.
     * @param id ID do prato.
     * @param dishDTO Dados textuais do prato.
     * @param file Novo arquivo de imagem (opcional).
     * @return O prato atualizado.
     */
    /**
     * 💡 NOVO MÉTODO DE ATUALIZAÇÃO CORRIGIDO:
     * Lida com MultipartFile (Upload) OU String (URL direta) OU remoção.
     */
    @Transactional
    public DishDTO update(Long id, DishDTO dto, MultipartFile file) {
        Dish existingDish = dishRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prato não encontrado: " + id));

        try {
            // 1. Atualiza campos textuais e preço (excluindo 'favorite')

            // Atualiza campos não nulos do DTO
            if (dto.getName() != null) existingDish.setName(dto.getName());
            if (dto.getCategory() != null) existingDish.setCategory(dto.getCategory());
            if (dto.getDescription() != null) existingDish.setDescription(dto.getDescription());
            // ⚠️ LINHA REMOVIDA: Não atualiza existingDish.setFavorite(dto.getFavorite());

            String oldUrlImage = existingDish.getUrlImage();
            String newUrlImageFromDto = dto.getUrlImage();
            boolean imageUpdated = false;

            // 2. Lógica de Atualização/Substituição da Imagem
            if (file != null && !file.isEmpty()) {
                // Caso A: Novo arquivo foi enviado (Prioridade máxima)

                // [Lógica opcional para DELETAR a imagem antiga do storage aqui]

                // Faz o upload do NOVO arquivo e obtém a URL
                String uploadedUrl = storageService.store(file);
                existingDish.setUrlImage(uploadedUrl);
                imageUpdated = true;

            }

            // 3. Verifica se houve alteração APENAS na URL do DTO (link externo ou remoção)
            if (!imageUpdated) {

                if (newUrlImageFromDto != null && !newUrlImageFromDto.equals(oldUrlImage)) {
                    // Caso B: Nova URL externa OU alteração da URL.

                    // [Lógica opcional para DELETAR o arquivo antigo do storage aqui, se for um arquivo próprio]

                    // Salva a nova URL (pode ser um link externo ou string vazia/null para remover)
                    existingDish.setUrlImage(newUrlImageFromDto.isEmpty() ? null : newUrlImageFromDto);

                } else if ((newUrlImageFromDto == null || newUrlImageFromDto.isEmpty()) && oldUrlImage != null) {
                    // Caso C: Remoção explícita (DTO enviou null/vazio, mas o DB tinha um link)

                    // [Lógica opcional para DELETAR o arquivo antigo do storage aqui]

                    existingDish.setUrlImage(null);
                }
            }
            // Se nenhum dos casos acima for verdadeiro, existingDish.imageUrl permanece o mesmo.

            // 4. Salva o prato com todas as alterações
            Dish saved = dishRepository.save(existingDish);
            return DishDTO.fromModel(saved);

        } catch (Exception e) {
            // Captura erros (ex: falha no upload) e devolve um 400 Bad Request
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao atualizar prato ou imagem", e);
        }
    }


    // --- Deleção ---

    public void delete(Long id) {
        if (!dishRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prato não encontrado para deletar");
        }
        dishRepository.deleteById(id);
    }
}