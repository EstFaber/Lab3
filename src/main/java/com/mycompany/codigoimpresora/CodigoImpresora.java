
package com.mycompany.codigoimpresora;


import java.util.concurrent.*;
import java.util.Scanner;

public class CodigoImpresora {

    static class Documento {
        final String nombre;
        final int paginas;
        Documento(String nombre, int paginas) {
            this.nombre = nombre;
            this.paginas = paginas;
        }
        @Override
        public String toString() {
            return nombre + " (" + paginas + " pag.)";
        }
    }

    static class Impresora implements Runnable {
        private final BlockingQueue<Documento> cola;
        private final int tiempoPorPaginaMs;
        private volatile boolean encendida = true;

        Impresora(BlockingQueue<Documento> cola, int tiempoPorPaginaMs) {
            this.cola = cola;
            this.tiempoPorPaginaMs = tiempoPorPaginaMs;
        }

        public void apagar() {
            encendida = false;
        }

        @Override
        public void run() {
            boolean mostradoEnEspera = false;
            try {
                while (encendida || !cola.isEmpty()) {
                    Documento doc = cola.poll(500, TimeUnit.MILLISECONDS);
                    if (doc == null) {
                        if (!mostradoEnEspera) {
                            System.out.println("[Impresora] En espera... (sin documentos)");
                            mostradoEnEspera = true;
                        }
                        continue;
                    }
                    mostradoEnEspera = false;
                    System.out.println("[Impresora] Comienza a imprimir: " + doc);
                    for (int p = 1; p <= doc.paginas; p++) {
                        System.out.printf("[Impresora] %s -> pagina %d de %d%n",
                                          doc.nombre, p, doc.paginas);
                        try {
                            Thread.sleep(tiempoPorPaginaMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            System.out.println("[Impresora] Interrumpida.");
                            return;
                        }
                    }
                    System.out.println("[Impresora] Terminado: " + doc);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Impresora] Finalizo por interrupcion.");
            }
            System.out.println("[Impresora] Apagada.");
        }
    }

    public static void main(String[] args) {
        BlockingQueue<Documento> cola = new LinkedBlockingQueue<>();
        int tiempoPorPaginaMs = 1000;
        Impresora impresora = new Impresora(cola, tiempoPorPaginaMs);
        Thread hiloImpresora = new Thread(impresora, "HiloImpresora");
        hiloImpresora.start();

        Scanner sc = new Scanner(System.in);
        System.out.println("Simulacion impresora (comandos: agregar <nombre> <paginas>, estado, salir)");
        while (true) {
            System.out.print("> ");
            String linea = sc.nextLine().trim();
            if (linea.isEmpty()) continue;
            String[] partes = linea.split("\\s+");
            String comando = partes[0].toLowerCase();
            try {
                if (comando.equals("agregar") && partes.length >= 3) {
                    String nombre = partes[1];
                    int paginas = Integer.parseInt(partes[2]);
                    if (paginas <= 0) {
                        System.out.println("Numero de paginas debe ser mayor que 0.");
                        continue;
                    }
                    Documento doc = new Documento(nombre, paginas);
                    cola.add(doc);
                    System.out.println("Documento agregado a la cola: " + doc);
                } else if (comando.equals("estado")) {
                    System.out.println("Cola actual: " + cola);
                } else if (comando.equals("salir")) {
                    System.out.println("Cerrando impresora...");
                    impresora.apagar();
                    hiloImpresora.join();
                    System.out.println("Programa terminado.");
                    break;
                } else {
                    System.out.println("Comando no reconocido. Usa: agregar <nombre> <paginas>, estado, salir");
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Error: paginas debe ser un numero entero.");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        sc.close();
    }
}

