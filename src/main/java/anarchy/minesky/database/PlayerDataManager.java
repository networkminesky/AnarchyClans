package anarchy.minesky.database;

import java.util.UUID;
import java.util.function.Consumer;

public class PlayerDataManager {

    /**
     * Retorna o status do Spy via callback (sempre true).
     */
    public void getSpy(UUID uuid, Consumer<Boolean> callback) {
        if (callback != null) {
            callback.accept(true);
        }
    }

    /**
     * Sobrecarga síncrona caso algum código prefira chamar direto sem callback.
     */
    public boolean getSpy(UUID uuid) {
        return true;
    }

    /**
     * Retorna as kills via callback (sempre 0).
     */
    public void getKills(UUID uuid, Consumer<Integer> callback) {
        if (callback != null) {
            callback.accept(0);
        }
    }

    /**
     * Retorna as mortes via callback (sempre 0).
     */
    public void getDeaths(UUID uuid, Consumer<Integer> callback) {
        if (callback != null) {
            callback.accept(0);
        }
    }
}