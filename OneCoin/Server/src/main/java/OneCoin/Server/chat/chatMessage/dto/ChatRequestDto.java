package OneCoin.Server.chat.chatMessage.dto;

import OneCoin.Server.chat.constant.MessageType;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatRequestDto {
    @NotBlank
    private MessageType type;
    @NotBlank
    private String userDisplayName;
    @NotBlank
    private Long userId;
    @NotBlank
    private Integer chatRoomId;
    @NotBlank
    private String message;


    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }
}