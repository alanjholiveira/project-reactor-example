package br.com.reactor.app;

import lombok.*;

@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class Anime {

    private String title;
    private String studio;
    private int episodes;

}
