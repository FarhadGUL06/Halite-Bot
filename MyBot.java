import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MyBot {
    InitPackage iPackage;
    int myID;
    GameMap gameMap;
    int numberOfFrames;
    ArrayList<Location> cells;
    int minX, maxX, minY, maxY;

    public MyBot () {
        iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;
        cells = new ArrayList<>();
        numberOfFrames = 1;
        minX = gameMap.width;
        maxX = 0;
        minY = gameMap.height;
        maxY = 0;
    }

    void update_coord(Location besLocation) {
        int XBest = besLocation.getX();
        int YBest = besLocation.getY();
        if (XBest < minX) {
            minX = XBest;
        }
        if (XBest > maxX) {
            maxX = XBest;
        }
        if (YBest < minY) {
            minY = YBest; 
        }
        if (YBest > maxY) {
            maxY = YBest; 
        }
    }

    void start_location () {
        // Plecarea de pe loc
        for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    final Site site = location.getSite();
                    if (site.owner == myID) {
                        minX = x;
                        maxX = x;
                        minY = y;
                        maxY = y;
                        cells.add(location);
                        break;
                    }
                }
            }
    }
    class pereche_timp_dir {
        int timp;
        int descoperire;
        public pereche_timp_dir(int timp, int descoperire) {
            this.descoperire = descoperire;
            this.timp = timp;
        }
    }  
    double getRaport(Location loc) {
        return ((double) loc.getSite().production / (double) loc.getSite().strength);
    }  
    int inside_me (Location location) {
        Location bestLocation = location;
        if (location.getSite().strength < 30) {
            return -1;
        }
        Queue <Location> PQ = new LinkedList<Location>();
        Map<Location, pereche_timp_dir > discoveryTimeForEachLocation = new HashMap<Location, pereche_timp_dir >();
        int distMin = 9999999;
        PQ.add(location);
        discoveryTimeForEachLocation.put(location, new pereche_timp_dir(0, 0));

        while(!PQ.isEmpty()) {
            // locQ = locatia scoasa din coada
            // peek = head, primul nod din coada
            Location locVecin = PQ.peek();
            PQ.remove();
            // daca e o solutie mai proasta decat una deja obtinuta, renuntam la ea
            if (discoveryTimeForEachLocation.get(locVecin).timp >= distMin)
                continue;
            
            for (int i = 0; i <= 3; i++) {
                // locQ = locatia derivata in directia i din locVecin 
                Location locQ = gameMap.getLocation(locVecin, Direction.CARDINALS[i]);
                if (locVecin.getSite().owner == myID) {
                pereche_timp_dir PTD;
                // daca deja am descoperit celula derivata
                // verificam daca aceasta are un timp mai bun decat timpul deja descoperit
                // caz in care il modificam
                if (discoveryTimeForEachLocation.containsKey(locQ)) {
                    if (discoveryTimeForEachLocation.get(locVecin).timp + 1 <
                        discoveryTimeForEachLocation.get(locVecin).timp) {
                        pereche_timp_dir perAux = new pereche_timp_dir
                                                  (discoveryTimeForEachLocation.get(locVecin).timp + 1,
                                                   discoveryTimeForEachLocation.get(locVecin).descoperire);
                        discoveryTimeForEachLocation.replace(locQ, perAux);
                        PQ.add(locQ);
                    }
                } else {
                    // daca e vecinul sursei de la care am plecat
                    // aici e problema V
                    if (locVecin.getX() == location.getX() && locVecin.getY() == location.getY())
                        PTD = new pereche_timp_dir(discoveryTimeForEachLocation.get(locVecin).timp + 1, i);
                    else 
                        PTD = new pereche_timp_dir
                             (discoveryTimeForEachLocation.get(locVecin).timp + 1, 
                             discoveryTimeForEachLocation.get(locVecin).descoperire);
                    if (!discoveryTimeForEachLocation.containsKey(locQ))
                        discoveryTimeForEachLocation.put(locQ, PTD);
                    PQ.add(locQ);
                }
                } // if is inside , cautam in continuare
                else {
                    if (discoveryTimeForEachLocation.containsKey(locQ))
                        if (discoveryTimeForEachLocation.get(locQ).timp == distMin) {
                            if (getRaport(locQ) > getRaport(bestLocation))
                                bestLocation = locQ;
                        }
                    if (discoveryTimeForEachLocation.containsKey(locQ))
                        if (discoveryTimeForEachLocation.get(locQ).timp < distMin) {
                            distMin = discoveryTimeForEachLocation.get(locQ).timp; 
                            bestLocation = locQ;
                        }
                } // altfel, verificam solutia obtinuta
            }
        }
        return discoveryTimeForEachLocation.get(bestLocation).descoperire;
        /* 
            Idei:
            - observam daca suntem atacati. retinem in ceva pozitiile
            - cand se intra aici, se verifica lista de amenintari (bataie in desfasurare)
            - ne mutam spre directia amenintarii in formatie de baricada (o sa gandim)
            - facem partea cu 0 amenintari
        */
    }

    boolean is_inside_me (Location location) {
        for (int i = 0; i <= 3; i++) {
            Location neigh = gameMap.getLocation(location, Direction.CARDINALS[i]);
            Site neighSite = neigh.getSite();
            if (neighSite.owner != myID) {
                return false;
            }
        }
        return true;
    }

    public void initial () {
        // m-am intors
        // Desfasurarea jocului efectiva
        Networking.sendInit("OdoBot");
        start_location();
        while(true) {
            List<Move> moves = new ArrayList<Move>();
            ArrayList<Location> newCells = new ArrayList<>();
            Networking.updateFrame(gameMap);
            int i, bestJ = 0;
            for (i = cells.size() - 1; i >= 0; i--) {
                final Location location = cells.get(i);
                final Site site = location.getSite();

                if (site.owner != myID) {
                    //cells.remove(i);
                    //i--;
                    continue;
                }
                // Verificam daca o celula este inauntru
                if (is_inside_me(location)) {
                    // Este inauntru
                    int res = inside_me(location);
                    if (res == -1) {
                        moves.add(new Move(location, Direction.STILL));
                        continue;
                    } else {
                        moves.add(new Move(location, Direction.CARDINALS[res]));
                    }
                } else {
                    Location bestLocation = location;
                    int scoreEnemy = Integer.MAX_VALUE;
                    double scoreNeutral = 0.0;
                    if (site.strength < 6 * site.production) {
                        moves.add(new Move(location, Direction.STILL));
                        continue;
                    }
                    for (int j = 0; j <= 3; j++) {
                        final Location loc = gameMap.getLocation(location, Direction.CARDINALS[j]);
                        final Site sitDirectie = loc.getSite();
                        // verificare existenta inamic
                        if (sitDirectie.owner != myID && sitDirectie.owner != 0 
                                && sitDirectie.strength < site.strength) {
                                if (sitDirectie.strength < scoreEnemy) {
                                    bestLocation = loc;
                                    bestJ = j;
                                }
                                continue;
                        }
                        if (sitDirectie.owner == 0) {
                            // obtinerea celei mai bune celule neutre
                            if ((double) sitDirectie.production / 
                                (double) sitDirectie.strength > scoreNeutral && 
                                sitDirectie.strength < site.strength) {
                                if (scoreEnemy == Integer.MAX_VALUE) {
                                    bestLocation = loc;
                                    scoreNeutral = (double) sitDirectie.production / 
                                                    (double) sitDirectie.strength;
                                    bestJ = j;
                                }
                            }
                        }
                    }
                    if (scoreEnemy == Integer.MAX_VALUE && scoreNeutral == 0) {
                        moves.add(new Move(location, Direction.STILL));
                    }
                    else {
                        final Site bestSite = bestLocation.getSite();
                        if (bestSite.owner != myID) {
                            newCells.add(bestLocation);
                            bestSite.owner = myID;
                            // Actualizam extremitatile
                            update_coord(bestLocation);
                        }
                        moves.add(new Move(location, Direction.CARDINALS[bestJ]));
                    }
                }
            }
            cells.addAll(newCells);
            newCells.clear();
            Networking.sendFrame(moves);
            numberOfFrames = numberOfFrames + 1;
        }
    }

    public static void main(String[] args) throws java.io.IOException {
        MyBot mybot = new MyBot();
        mybot.initial();
    }
}
