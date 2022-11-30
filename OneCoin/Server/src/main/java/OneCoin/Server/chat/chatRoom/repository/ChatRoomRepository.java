package OneCoin.Server.chat.chatRoom.repository;

import OneCoin.Server.chat.chatRoom.utils.ChatRoomUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {
    private final ChatRoomUtils chatRoomUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private SetOperations<String, String> setOperations;// <KEY_FOR_CHAT_ROOMS, ChatRoomIdKey>


    @PostConstruct
    private void init() {
        setOperations = redisTemplate.opsForSet();
    }

    public void create(Integer chatRoomId) {
        setOperations.add(chatRoomUtils.KEY_FOR_CHAT_ROOMS, chatRoomUtils.getKey(chatRoomId));
    }

    public Set<String> findAll() {
        return setOperations.members(chatRoomUtils.KEY_FOR_CHAT_ROOMS);
    }

    public boolean contains(Integer chatRoomId) {
        return setOperations.isMember(chatRoomUtils.KEY_FOR_CHAT_ROOMS, chatRoomUtils.getKey(chatRoomId));
    }

    public void delete(Integer chatRoomId) {
        setOperations.remove(chatRoomUtils.KEY_FOR_CHAT_ROOMS, chatRoomUtils.getKey(chatRoomId));
    }
}
