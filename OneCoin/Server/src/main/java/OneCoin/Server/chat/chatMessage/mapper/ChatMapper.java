package OneCoin.Server.chat.chatMessage.mapper;

import OneCoin.Server.chat.chatMessage.dto.ChatRequestDto;
import OneCoin.Server.chat.chatMessage.dto.ChatResponseDto;
import OneCoin.Server.chat.chatMessage.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {
    ChatMessage requestDtoToChatMessage(ChatRequestDto chatRequestDto);

    ChatResponseDto chatMessageToResponseDto(ChatMessage chatMessage);

    List<ChatResponseDto> chatMessagesToResponseDtos(List<ChatMessage> chatMessage);

}
