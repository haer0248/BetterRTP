package me.SuperRonanCraft.BetterRTP.player.rtp;

import io.papermc.lib.PaperLib;
import me.SuperRonanCraft.BetterRTP.Main;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RTPTeleport {

    private final RTPParticles eParticles = new RTPParticles();
    private final RTPPotions ePotions = new RTPPotions();
    private final RTPSounds eSounds = new RTPSounds();
    private final RTPTitles eTitles = new RTPTitles();

    void load() {
        eParticles.load();
        ePotions.load();
        eSounds.load();
        eTitles.load();
    }

    void sendPlayer(final CommandSender sendi, final Player p, final Location loc, final int price,
                    final int attempts) throws NullPointerException {
        if (sendi != p) //Tell sendi that the player will/is being rtp'd
            sendSuccessMsg(sendi, p.getDisplayName(), loc, price, false, attempts);
        getPl().getText().getSuccessLoading(sendi); //Send loading message
        List<CompletableFuture<Chunk>> asyncChunks = getChunks(loc); //Get a list of chunks
        CompletableFuture.allOf(asyncChunks.toArray(new CompletableFuture[] {})).thenRun(() -> { //Async chunk load
            try {
                PaperLib.teleportAsync(p, loc).thenRun(new BukkitRunnable() { //Async teleport
                    @Override
                    public void run() {
                        afterTeleport(p, loc, price, attempts);
                        getPl().getCmd().rtping.remove(p.getUniqueId()); //No longer rtp'ing
                    }
                });
            } catch (Exception e) {
                getPl().getCmd().rtping.remove(p.getUniqueId()); //No longer rtp'ing (errored)
                e.printStackTrace();
            }
        });
    }

    public void afterTeleport(Player p, Location loc, int price, int attempts) { //Only a successful rtp should run this OR '/rtp test'
        eSounds.playTeleport(p);
        eParticles.display(p);
        ePotions.giveEffects(p);
        eTitles.showTeleport(p, loc, attempts);
        sendSuccessMsg(p, p.getDisplayName(), loc, price, true, attempts);
    }

    public void beforeTeleport(Player p, int delay) { //Only Delays should call this
        eSounds.playDelay(p);
        eTitles.showDelay(p, p.getLocation());
        getPl().getText().getDelay(p, delay);
    }

    private List<CompletableFuture<Chunk>> getChunks(Location loc) { //List all chunks in range to load
        List<CompletableFuture<Chunk>> asyncChunks = new ArrayList<>();
        int range = 5;
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                Location locLoad = new Location(loc.getWorld(), loc.getX() + (x * 16), loc.getY(), loc.getZ() + (z * 16));
                CompletableFuture<Chunk> chunk = PaperLib.getChunkAtAsync(locLoad, true);
                asyncChunks.add(chunk);
            }
        }
        return asyncChunks;
    }

    private void sendSuccessMsg(CommandSender sendi, String player, Location loc, int price, boolean sameAsPlayer,
                                int attempts) {
        String x = Integer.toString(loc.getBlockX());
        String y = Integer.toString(loc.getBlockY());
        String z = Integer.toString(loc.getBlockZ());
        String world = loc.getWorld().getName();
        if (sameAsPlayer) {
            if (price == 0)
                getPl().getText().getSuccessBypass(sendi, x, y, z, world, attempts);
            else
                getPl().getText().getSuccessPaid(sendi, price, x, y, z, world, attempts);
        } else
            getPl().getText().getOtherSuccess(sendi, player, x, y, z, world, attempts);
    }

    private Main getPl() {
        return Main.getInstance();
    }
}
