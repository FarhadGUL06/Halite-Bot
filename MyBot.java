import java.util.ArrayList;
import java.util.List;

public class MyBot {
    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        final int myID = iPackage.myID;
        final GameMap gameMap = iPackage.map;

        ArrayList<Location> cells = new ArrayList<>();

        Networking.sendInit("MyJavaBot");
        int check = 0;
        int dx[] = {-1, 0, 1, 0};
        int dy[] = {0, -1, 0, 1};
        while(true) {
            List<Move> moves = new ArrayList<Move>();
            ArrayList<Location> newCells = new ArrayList<>();
            Networking.updateFrame(gameMap);

            for (int y = 0; y < gameMap.height && check == 0; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    final Site site = location.getSite();
                    if (site.owner == myID) {
                        if (x == 0 || y == 0 || y == gameMap.height-1 || x == gameMap.width-1) {
                            //moves.add(new Move(location, Direction.STILL));
                            //continue;
                        }
                        cells.add(location);
                        check = 1;
                        break;
                    }
                }
            }

            if (check == 1) {
                int i, bestJ = 0;
                for (i = 0; i < cells.size();  i++) {
                    final Location location = cells.get(i);
                    final Site site = location.getSite();
                    Location bestLocation = location;
                    int scoreEnemy = Integer.MAX_VALUE;
                    float scoreNeutral = 0;
                    if (site.owner == myID) {
                        if (site.strength < 6 * site.production) {
                            moves.add(new Move(location, Direction.STILL));
                            continue;
                        }
                        for (int j = 0; j <= 3; j++) {
                            final Location loc = gameMap.getLocation(location, Direction.CARDINALS[j]);
                            final Site sitDirectie = loc.getSite();
                            if (sitDirectie.owner != myID && sitDirectie.owner != 0 
                                    && sitDirectie.strength < site.strength) {
                                    if (sitDirectie.strength < scoreEnemy) {
                                        bestLocation = loc;
                                        bestJ = j;
                                    }
                                    continue;
                            }
                            if (sitDirectie.owner == 0) {
                                if ((float) sitDirectie.production / 
                                    (float) sitDirectie.strength > scoreNeutral && 
                                    sitDirectie.strength < site.strength) {
                                    if (scoreEnemy == Integer.MAX_VALUE) {
                                        bestLocation = loc;
                                        scoreNeutral = (float) sitDirectie.production / 
                                                       (float) sitDirectie.strength;
                                        bestJ = j;
                                    }
                                }
                            }
                        }
                        if (scoreEnemy == Integer.MAX_VALUE && scoreNeutral == 0)
                            moves.add(new Move(location, Direction.STILL));
                        else {
                       
                        final Site bestSite = bestLocation.getSite();
                        if (bestSite.owner != myID) {
                            newCells.add(bestLocation);
                            bestSite.owner = myID;
                        }
                        moves.add(new Move(location, Direction.CARDINALS[bestJ]));
                        }
                    }

                }
                cells.addAll(newCells);
                newCells.clear();
            }
            Networking.sendFrame(moves);
        }
    }
}
