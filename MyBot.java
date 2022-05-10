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
                int i;
                for (i = 0; i < cells.size();  i++) {
                    final Location location = cells.get(i);
                    final Site site = location.getSite();
                    final Location bestLocation;
                    int scoreEnemy = Integer.MAX_VALUE;
                    float scoreNeutral = 0;
                    if (site.strength < 6 * site.production) {
                        moves.add(new Move(location, Direction.STILL));
                        continue;
                    }
                    if (site.owner == myID) {
                        for (i = 0; i <= 3; i++) {
                            final Location loc = gameMap.getLocation(location, Direction.CARDINALS[i+1]);
                            final Site sitDirectie = loc.getSite();
                            if (sitDirectie.owner != myID && sitDirectie.owner != 0 
                                    && sitDirectie.strength < site.strength) {
                                    if (sitDirectie.strength < scoreEnemy)
                                        bestLocation = loc;
                                    continue;
                            }
                            if (sitDirectie.owner == 0) {
                                if ((float) sitDirectie.production / 
                                    (float) sitDirectie.strength > scoreNeutral) {
                                    if (scoreEnemy == Integer.MAX_VALUE)
                                        bestLocation = loc;
                                }
                            }
                        }
                        if (scoreEnemy == Integer.MAX_VALUE && scoreNeutral == 0)
                            moves.add(new Move(location, Direction.STILL));
                        else
                            moves.add(new Move(location, bestLocation));
                        /*
                        final Location left = gameMap.getLocation(location, Direction.WEST);
                        final Site leftCell = left.getSite();
    
                        final Location right = gameMap.getLocation(location, Direction.EAST);
                        final Site rightCell = right.getSite();
    
                        final Location up = gameMap.getLocation(location, Direction.NORTH);
                        final Site upCell = up.getSite();
    
                        final Location down = gameMap.getLocation(location, Direction.SOUTH);
                        final Site downCell = down.getSite();
                        
                        if (site.strength > 70) {
                            if (rightCell.strength < site.strength) {
                                moves.add(new Move(location, Direction.EAST));
                                if (rightCell.owner != myID) {
                                    rightCell.owner = myID;
                                    newCells.add(right);
                                }
                                continue;
                            }
                            if (downCell.strength < site.strength) {
                                moves.add(new Move(location, Direction.SOUTH));
                                if (downCell.owner != myID) {
                                    downCell.owner = myID;
                                    newCells.add(down);
                                }
                                continue;
                            }
                            if (upCell.strength < site.strength) {
                                moves.add(new Move(location, Direction.NORTH));
                                if (upCell.owner != myID) {
                                    upCell.owner = myID;
                                    newCells.add(up);
                                }
                                continue;
                            }
                            if (leftCell.strength < site.strength) {
                                moves.add(new Move(location, Direction.WEST));
                                if (leftCell.owner != myID) {
                                    leftCell.owner = myID;
                                    newCells.add(left);
                                }
                                continue;
                            }
                        }

                        if (leftCell.strength < site.strength && leftCell.owner!=myID) {
                            moves.add(new Move(location, Direction.WEST));
                            if (leftCell.owner != myID) {
                                leftCell.owner = myID;
                                newCells.add(left);
                            }
                            continue;
                        }
                        if (rightCell.strength < site.strength && rightCell.owner!=myID) {
                            moves.add(new Move(location, Direction.EAST));
                            if (rightCell.owner != myID) {
                                rightCell.owner = myID;
                                newCells.add(right);
                            }
                            continue;
                        }
                        if (upCell.strength < site.strength && upCell.owner!=myID) {
                            moves.add(new Move(location, Direction.NORTH));
                            if (upCell.owner != myID) {
                                upCell.owner = myID;
                                newCells.add(up);
                            }
                            continue;
                        }
                        if (downCell.strength < site.strength && downCell.owner != myID) {
                            moves.add(new Move(location, Direction.SOUTH));
                            if (downCell.owner != myID) {
                                downCell.owner = myID;
                                newCells.add(down);
                            }

                            continue;
                        }
                        */
                        //moves.add(new Move(location, Direction.STILL));
                    }

                }
                cells.addAll(newCells);
                newCells.clear();
            }
            Networking.sendFrame(moves);
        }
    }
}
