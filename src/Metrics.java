import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Metrics {

    private final AtomicLong firmaTotal        = new AtomicLong();
    private final AtomicLong cifTablaTotal     = new AtomicLong();
    private final AtomicLong verifTotal        = new AtomicLong();
    private final AtomicLong cifRespSymTotal   = new AtomicLong();
    private final AtomicLong cifRespAsymTotal  = new AtomicLong();
    private final AtomicInteger muestras       = new AtomicInteger();

    public void addFirma(long ns)   { firmaTotal.addAndGet(ns);   muestras.incrementAndGet(); }
    public void addCifTabla(long ns){ cifTablaTotal.addAndGet(ns);}
    public void addVerif(long ns)   { verifTotal.addAndGet(ns);   }
    public void addCifSym(long ns)  { cifRespSymTotal.addAndGet(ns);}
    public void addCifAsym(long ns) { cifRespAsymTotal.addAndGet(ns);}

    private double avg(AtomicLong total){ return total.get()/(muestras.get()*1_000_000.0); }

    public double getAvgFirma()    { return avg(firmaTotal);      }
    public double getAvgCifTabla() { return avg(cifTablaTotal);   }
    public double getAvgVerif()    { return avg(verifTotal);      }
    public double getAvgCifSym()   { return avg(cifRespSymTotal); }
    public double getAvgCifAsym()  { return avg(cifRespAsymTotal);}
    public int    getMuestras()    { return muestras.get();       }
}

