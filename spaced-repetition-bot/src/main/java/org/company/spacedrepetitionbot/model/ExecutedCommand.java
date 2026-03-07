package org.company.spacedrepetitionbot.model;

import jakarta.persistence.*;
import lombok.*;
import org.company.spacedrepetitionbot.utils.converter.StringArrayConverter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ExecutedCommand {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "command_seq")
    @SequenceGenerator(name = "command_seq", sequenceName = "command_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_chat_id", referencedColumnName = "userChatId")
    private UserInfo userInfo;

    private String commandIdentifier;

    @Convert(converter = StringArrayConverter.class)
    private String[] arguments;

    private LocalDateTime executedAt;
}
