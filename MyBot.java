import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class MyBot {
    InitPackage iPackage;
    int myID;
    GameMap gameMap;
    int numberOfFrames;
    ArrayList<Location> cells;
    HashMap<Location, Integer> isDanger;
    int minX, maxX, minY, maxY;
    int dx[] = {-1, 0, 1, 0};
    int dy[] = {0, 1, 0, -1};
    int[][] TableOfSums;
    int[][] TableOfStrength;
    int[][] TableOfAttack;
    boolean underAttack;
    class PerecheScor {
        Location loc;
        int score;
        public PerecheScor(Location loc, int score) {
            this.loc = loc;
            this.score = score;
        }
    }
    class PerecheScorComparator implements Comparator<PerecheScor> {
        @Override
        public int compare(MyBot.PerecheScor s1, MyBot.PerecheScor s2) {
            
            if (s1.score < s2.score) {
                return 1;
            }
            if (s1.score > s2.score) {
                return -1;
            }
            return 0;
        }
    }
    PriorityQueue <PerecheScor> pq = new PriorityQueue<PerecheScor>(10000, 
                                         new PerecheScorComparator());
    public MyBot () {
        iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;
        cells = new ArrayList<Location>();
        numberOfFrames = 1;
        minX = gameMap.width;
        maxX = 0;
        minY = gameMap.height;
        maxY = 0;
        isDanger = new HashMap<Location, Integer>();
        underAttack = false;
    } 
    boolean is_on_border (Location location) {
        Site site = location.getSite();
        if (site.owner != 0)
            return false;
        for (int i = 0; i <= 3; i++) {
            Location neigh = gameMap.getLocation(location, Direction.CARDINALS[i]);
            Site neighSite = neigh.getSite();
            if (neighSite.owner == myID) {
                return true;
            }
        }
        return false;
    }
    boolean ally_on_border (Location location) {
        Site site = location.getSite();
        // nu e a mea => n-avem ce
        if (site.owner != myID)
            return false;
        for (int i = 0; i <= 3; i++) {
            Location neigh = gameMap.getLocation(location, Direction.CARDINALS[i]);
            Site neighSite = neigh.getSite();
            // vecin cu inamicul => not my problem, se ocupa altii
            if (neighSite.owner != 0 && neighSite.owner != myID) {
                return false;
            }
            // vecin cu celula neutra => ar putea fi celula care ne desparte 
            // de adversar
            if (neighSite.owner == 0) {
                for (int j = 0; j <= 3; j++) {
                    if (Math.abs(j - i) == 2) 
                        continue;
                    Location anotherLoc = gameMap.getLocation(neigh, Direction.CARDINALS[j]);
                    Site anotherSite = anotherLoc.getSite();
                    if (anotherSite.owner != 0 && anotherSite.owner != myID) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public void UpdateTable() {
        int height = gameMap.height, width = gameMap.width;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                Location location = gameMap.getLocation(x, y);
                Site site = location.getSite();
                if (is_on_border(location)) {
                    TableOfSums[x][y] = site.production * 5 -
                                        (site.strength * 7 / 10) + 50;
                    PerecheScor PS = new PerecheScor(location, 
                                         TableOfSums[x][y]);
                    pq.add(PS);
                }
            }
        }
    }
    public void initTOS() {
        int height = gameMap.height, width = gameMap.width;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                TableOfSums[x][y] = 0;
                Location location = gameMap.getLocation(x, y);
                Site site = location.getSite();
                TableOfStrength[x][y] = site.strength;
            }
        }
    }

    public void calculateUtilProduction () {
        int x, y;
        int height = gameMap.height;
        int width = gameMap.width;
        for (y = 0; y < height; ++y) {
            for (x = 0; x < width; ++x) {
                Location location = gameMap.getLocation(x, y);
                Site site = location.getSite();
                // Eu sunt seful celulei
                if (site.owner == myID) {
                    TableOfSums[x][y] = 0;
                    continue;
                }
                // Vom ciurui o celula neutra
                int scoreCell = 0;
                int nrVec = 0;
                for (int i = 0; i < 4; ++i) {
                    Location locationNeigh = gameMap.getLocation(location, Direction.CARDINALS[i]);
                    Site siteNeigh = locationNeigh.getSite();
                    // Sunt seful pe celula vecina
                    if (siteNeigh.owner == myID) {
                        continue;
                    }
                    nrVec++;
                    scoreCell += siteNeigh.production * 50 - (siteNeigh.strength * 7 / 10);
                }
                // Actualizam scorul pentru celula curenta in functie de scorul vecinilor
                TableOfSums[location.getX()][location.getY()] = site.production * 50 - (site.strength * 7 / 10);
                if (nrVec != 0) {
                    TableOfSums[location.getX()][location.getY()] += (scoreCell / nrVec);
                }
            }
        }
    }

    public int calculateAttack (int myX, int myY) {
        // Functie care imi calculeaza distanta minima intre nodul curent si primul inamic
        int x, y;
        int direction = -1;
        int minDistance = 9999;
        int height = gameMap.height;
        int width = gameMap.width;
        // Locatia celulei mele curente
        Location location = gameMap.getLocation(myX, myY);
        for (y = 0; y < height; ++y) {
            for (x = 0; x < width; ++x) {
                // Locatia si site-ul inamic
                Location locationEnemy = gameMap.getLocation(x, y);
                Site siteEnemy = locationEnemy.getSite();
                if (siteEnemy.owner != myID && siteEnemy.owner != 0) {
                    // Am gasit o celula inamica
                    // Calculez care vecin este cel mai aproape de ea
                    for (int i = 0; i < 4; ++i) {
                        Location locationNeigh = gameMap.getLocation(location, Direction.CARDINALS[i]);
                        int distNeigh = Math.abs(locationNeigh.getY()-locationEnemy.getY()) + Math.abs(locationNeigh.getX()-locationEnemy.getX());
                        //int distNeigh = Math.abs( (Math.abs(height/2 - locationNeigh.getY())) - (Math.abs(height/2 - locationEnemy.getY())) ) +
                        //                Math.abs( (Math.abs(width/2 - locationNeigh.getX())) - (Math.abs(width/2 - locationEnemy.getX())) );
                        if (distNeigh < minDistance) {
                            minDistance = distNeigh;
                            direction = i;
                        }
                    }
                }
            }
        }
        return direction;
    }


    public void UpdateTableInside() {
        while(!pq.isEmpty()) {
            PerecheScor PsAux = pq.peek();
            pq.poll();
            Site site = PsAux.loc.getSite();
            for (int i = 0; i <= 3; ++i) {
                Location neigh = gameMap.getLocation
                (PsAux.loc, Direction.CARDINALS[i]);
                if(neigh.getSite().owner == myID)
                    {
                        int yy = neigh.getY();
                        int xx = neigh.getX();
                        int exX = PsAux.loc.getX();
                        int exY = PsAux.loc.getY();
                        if (TableOfSums[xx][yy] == 0) {
                            TableOfSums[xx][yy] = 
                            TableOfSums[exX][exY] - neigh.getSite().production 
                                                - 2;
                            PerecheScor BackToPQ = new PerecheScor(neigh,
                            TableOfSums[xx][yy]);
                            pq.add(BackToPQ);
                        }
                    }
            }
        }
    }
    class PairForJavaSeven {
        int scor;
        int direction;
        public PairForJavaSeven(int direction, int scor) {
            this.scor = scor;
            this.direction = direction;
        }
    }
    class PairForJavaSevenComparator implements Comparator<PairForJavaSeven> {
        @Override
        public int compare(MyBot.PairForJavaSeven s1, MyBot.PairForJavaSeven s2) {
            
            if (s1.scor < s2.scor) {
                return 1;
            }
            if (s1.scor > s2.scor) {
                return -1;
            }
            return 0;
        }
    }
    public int countAllyCells(int dir, Location loc) {
        Location auxLoc = gameMap.getLocation(loc, Direction.CARDINALS[dir]);
        Site site = auxLoc.getSite();
        int count = 0;
        while (site.owner == myID) {
            ++count;
            auxLoc = gameMap.getLocation(auxLoc, Direction.CARDINALS[dir]);
            if (count > Math.max(gameMap.height, gameMap.width))
                return 999999;
            site = auxLoc.getSite();
        }
        return count;
    }
    public int cross(Location loc) {
        int i, minCount = 999999, bestDirection = -1, auxValue;
        for (i = 0; i <= 3; ++i) {
            auxValue = countAllyCells(i, loc);
            if (auxValue < minCount) {
                minCount = auxValue;
                bestDirection = i; 
            }
        }
        return bestDirection;
    }
boolean ItsTimeToStop(Location location) {
    int dangerType = 99;
    for (int i = 0; i <= 3; i++) {
            // Vecinii nodului curent
            Location neighOrd1 = gameMap.getLocation(location, Direction.CARDINALS[i]);
            Site neighSite1 = neighOrd1.getSite();
                if (neighSite1.owner != myID && neighSite1.owner != 0) {
                    // Vecinul de ordinul 1 este inamic!
                    isDanger.put(location, i);
                    dangerType = 1;
                    break;
                    //dangerType = Math.min(dangerType, 1);
                }
            //}
             
            for (int j = 0; j <= 3; j++) {
                // Vecinii vecinilor nodului curent
                Location neighOrd2 = gameMap.getLocation(neighOrd1, Direction.CARDINALS[j]);
                Site neighSite2 = neighOrd2.getSite();
                //if (!isDanger.containsKey(location)) {
                    if (neighSite2.owner != myID && neighSite2.owner != 0) {
                        // Vecinul de ordinul 2 este inamic!
                        if (!isDanger.containsKey(location)) {
                            isDanger.put(location, i);
                        }
                        dangerType = Math.min(dangerType, 2);
                        //dangerType = 2;
                    }
            }
    }
    return true;
}
public int nearEnemy(Location loc) {
    int i, minVal = 9999, minIndex = 10;
    Site site = loc.getSite();
    Site AuxSite;
    Location locAux;
    for (i = 0; i <= 3; ++i) {
        locAux = gameMap.getLocation(loc, Direction.CARDINALS[i]);
        AuxSite = locAux.getSite();
        if (AuxSite.owner == myID || AuxSite.owner == 0)
            continue;   
        underAttack = true;
        if (site.strength > AuxSite.strength) {
                minVal = AuxSite.strength;
                minIndex = i;
        }
    }
    return minIndex;
}
    public void initial () {
        // Desfasurarea jocului efectiva
        TableOfSums = new int[1000][1000];
        TableOfStrength = new int[1000][1000];

        Networking.sendInit("OdoBot");
        //start_location();
        while(true) {
        numberOfFrames++;
        List<Move> moves = new ArrayList<Move>();
        Networking.updateFrame(gameMap);
        // isDanger.clear();
        initTOS();
        UpdateTable();
        UpdateTableInside();
        //calculateUtilProduction();
         for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                int finalDirection = -1, maxScore = -99999, finalAdvancedDirection = -1, maxAdvancedScore = -99999;
                Location location = gameMap.getLocation(x, y);
                Site site = location.getSite();
                int checkEnemy;
                //PriorityQueue <PairForJavaSeven> PFJS = new PriorityQueue<PairForJavaSeven>(5, 
                  //                              new PairForJavaSevenComparator());
                if (site.owner != myID) {
                    continue;
                }
                if (site.production > 10 && site.strength < 10 * site.production) {
                    moves.add(new Move(location, Direction.STILL));
                    continue;
                }
                if (site.strength < 6 * site.production || site.strength < 30) {
                    moves.add(new Move(location, Direction.STILL));
                    continue;
                }
                if (underAttack) {
                    int directionAttack = calculateAttack(x, y);
                    //moves.add(new Move(location, Direction.CARDINALS[directionAttack]));
                    //continue;
                }
                if (numberOfFrames > gameMap.width*2) {
                    checkEnemy = nearEnemy(location);
                    // miscare celula vecina cu inamic
                    if (checkEnemy != 10) {
                        moves.add(new Move(location, Direction.CARDINALS[checkEnemy]));
                        continue;
                    }
                    int FrameBigDir = cross(location);
                    if (FrameBigDir == -1) {
                        moves.add(new Move(location, Direction.STILL));
                    } else {
                      //  Location locAux = gameMap.getLocation(location, Direction.CARDINALS[FrameBigDir]);
                    //if (ally_on_border(location) && locAux.getSite().strength > 10)
                    //    moves.add(new Move(location, Direction.STILL));
                    //else
                        moves.add(new Move(location, Direction.CARDINALS[FrameBigDir]));
                    }
                    continue;
                }
                for (int i = 0; i <= 3; ++i) {
                    Location LocAux = gameMap.getLocation(location, Direction.CARDINALS[i]);
                    if (TableOfSums[LocAux.getX()][LocAux.getY()] > maxScore) {
                        maxScore = TableOfSums[LocAux.getX()][LocAux.getY()];
                        finalDirection = i;
                    }
                    //PairForJavaSeven pair = new PairForJavaSeven(i, TableOfSums[LocAux.getX()][LocAux.getY()]);
                    //PFJS.add(pair);
                }
                Location newLoc;
                newLoc = gameMap.getLocation(location, Direction.CARDINALS[finalDirection]);
                if (site.strength <= newLoc.getSite().strength) {
                    moves.add(new Move(location, Direction.STILL));
                    continue;
                }
                moves.add(new Move(location, Direction.CARDINALS[finalDirection]));
                /*Site newSite;
                int i;
                PairForJavaSeven sch = new PairForJavaSeven(0, 0);
                for (i = 0; i <= 3; i++) {
                    //sch = PFJS.peek();
                    newLoc = gameMap.getLocation(location, Direction.CARDINALS[sch.direction]);
                    newSite = newLoc.getSite();
                    if (site.strength <= newLoc.getSite().strength) {
                        continue;
                    }

                    if (site.strength + TableOfStrength[newLoc.getX()][newLoc.getY()] > 255 && newSite.owner == myID) {
                        continue;    
                    }
                    //PFJS.poll();
                    break;
                }
                //PFJS.clear();
                if (i == 4) {
                    moves.add(new Move(location, Direction.STILL));
                } else {
                    newLoc = gameMap.getLocation(location, Direction.CARDINALS[sch.direction]);
                    TableOfStrength[newLoc.getX()][newLoc.getY()] += site.strength;
                    TableOfStrength[location.getX()][location.getY()] = 0;
                    moves.add(new Move(location, Direction.CARDINALS[sch.direction]));
                }*/
            } // al doilea for
         } // primul for
        Networking.sendFrame(moves);
         } // while (true)
    } // final functie
    public static void main(String[] args) throws java.io.IOException {
        MyBot mybot = new MyBot();
        mybot.initial();
    }
}
