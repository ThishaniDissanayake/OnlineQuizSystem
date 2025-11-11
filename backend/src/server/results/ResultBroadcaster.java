package server.results;

import java.util.concurrent.*;


public class ResultBroadcaster {
    
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    

    public static CompletableFuture<Void> broadcastScoresAsync() {
        return CompletableFuture.runAsync(() -> {
            System.out.println("\n📊 [ASYNC] BROADCAST: Sending individual scores...");
            
            try {
                ScoreDistributor.distributeScoredToAllClients();
                System.out.println("✅ [ASYNC] All score broadcasts completed");
            } catch (Exception e) {
                System.err.println("❌ [ASYNC] Error during score broadcast: " + e.getMessage());
            }
        }, EXECUTOR);
    }
    

    public static CompletableFuture<Void> broadcastLeaderboardAsync() {
        return CompletableFuture.runAsync(() -> {
            System.out.println("\n🏆 [ASYNC] BROADCAST: Sending leaderboard...");
            
            try {
                ScoreDistributor.broadcastFinalLeaderboard();
                System.out.println("✅ [ASYNC] All leaderboard broadcasts completed");
            } catch (Exception e) {
                System.err.println("❌ [ASYNC] Error during leaderboard broadcast: " + e.getMessage());
            }
        }, EXECUTOR);
    }
    

    public static CompletableFuture<Void> distributeResultsAsync() {
        return CompletableFuture.runAsync(() -> {
            System.out.println("\n" + "🚀".repeat(30));
            System.out.println("[ASYNC] STARTING ASYNC RESULT DISTRIBUTION");
            System.out.println("🚀".repeat(30) + "\n");
            
            try {

                System.out.println("Step 1/3: Distributing individual scores...");
                ScoreDistributor.distributeScoredToAllClients();
                

                System.out.println("\nStep 2/3: Broadcasting leaderboard...");
                ScoreDistributor.broadcastFinalLeaderboard();
                

                System.out.println("\nStep 3/3: Printing results board...");
                ScoreDistributor.printLeaderboard();
                
                System.out.println("\n" + "✅".repeat(30));
                System.out.println("[ASYNC] ASYNC RESULT DISTRIBUTION COMPLETE");
                System.out.println("✅".repeat(30) + "\n");
                
            } catch (Exception e) {
                System.err.println("❌ [ASYNC] Error during async distribution: " + e.getMessage());
            }
        }, EXECUTOR);
    }
    

    public static void shutdown() {
        try {
            if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
                EXECUTOR.shutdown();
                if (!EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("⚠️ [ASYNC] Executor did not terminate gracefully, forcing shutdown...");
                    EXECUTOR.shutdownNow();
                }
            }
            
            System.out.println("✅ [ASYNC] ResultBroadcaster shutdown complete");
        } catch (InterruptedException e) {
            System.err.println("⚠️ [ASYNC] Interrupted during shutdown: " + e.getMessage());
            EXECUTOR.shutdownNow();
        }
    }
    

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }
}
