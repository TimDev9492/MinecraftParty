package me.timwastaken.minecraftparty;

import me.timwastaken.minecraftparty.models.other.MinigameUtils;

import java.util.*;
import java.util.function.Function;

public class Testing {

    public static void main(String[] args) {
        HashMap<UUID, Integer> gamesPlayed = new HashMap<>();
        Function<Integer, Integer> valueGenerator = input -> (input + 4) / 5;
        for (int i = 0; i < 10; i++) {
            UUID id = UUID.randomUUID();
            int value = valueGenerator.apply(i);
            gamesPlayed.put(id, value);
            System.out.println(id + " -> " + value);
        }
        UUID[] result = MinigameUtils.getNewFightingPair(gamesPlayed);
        System.out.println(Arrays.toString(result));
    }

}
