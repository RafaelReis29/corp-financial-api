package com.senac.corpfinancialapi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(CompanyRepository companyRepository, ContactRepository contactRepository, ContractRepository contractRepository, PaymentMethodRepository paymentMethodRepository, InvoiceRepository invoiceRepository) {
        return args -> {
            LocalDateTime now = LocalDateTime.now();

            Company stark = companyRepository.save(new Company("Industrias Stark", "Industrias Stark S.A.", "10001001000101", "Tecnologia e Defesa", "contato@starkindustries.com.br", "11990010001", true, now, now));
            log.info("Preloading " + stark);
            log.info("Preloading " + contactRepository.save(new Contact(stark.getId(), "Tony Stark", "Diretor Executivo", "tony.stark@starkindustries.com.br", "11990010002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(stark.getId(), "Pepper Potts", "Diretora Executiva", "pepper.potts@starkindustries.com.br", "11990010003", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(stark.getId(), "Happy Hogan", "Chefe de Seguranca", "happy.hogan@starkindustries.com.br", "11990010004", false, true, now, now)));

            Company wayne = companyRepository.save(new Company("Empresas Wayne", "Empresas Wayne S.A.", "10001002000102", "Tecnologia e Energia", "contato@wayne.com.br", "11990020001", true, now, now));
            log.info("Preloading " + wayne);
            log.info("Preloading " + contactRepository.save(new Contact(wayne.getId(), "Bruce Wayne", "Presidente do Conselho", "bruce.wayne@wayne.com.br", "11990020002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(wayne.getId(), "Lucius Fox", "Diretor de Tecnologia", "lucius.fox@wayne.com.br", "11990020003", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(wayne.getId(), "Alfred Pennyworth", "Gerente de Operacoes", "alfred@wayne.com.br", "11990020004", false, true, now, now)));

            Company oscorp = companyRepository.save(new Company("Oscorp", "Oscorp Industrias S.A.", "10001003000103", "Biotecnologia e Quimica", "contato@oscorp.com.br", "11990030001", true, now, now));
            log.info("Preloading " + oscorp);
            log.info("Preloading " + contactRepository.save(new Contact(oscorp.getId(), "Norman Osborn", "Diretor Executivo", "norman.osborn@oscorp.com.br", "11990030002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(oscorp.getId(), "Harry Osborn", "Vice-Presidente", "harry.osborn@oscorp.com.br", "11990030003", false, true, now, now)));

            Company umbrella = companyRepository.save(new Company("Umbrella", "Corporacao Umbrella Ltda.", "10001004000104", "Farmaceutica", "contato@umbrella.com.br", "11990040001", true, now, now));
            log.info("Preloading " + umbrella);
            log.info("Preloading " + contactRepository.save(new Contact(umbrella.getId(), "Albert Wesker", "Diretor de Pesquisa", "albert.wesker@umbrella.com.br", "11990040002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(umbrella.getId(), "William Birkin", "Cientista Chefe", "william.birkin@umbrella.com.br", "11990040003", false, true, now, now)));

            Company ingen = companyRepository.save(new Company("InGen", "InGen Biotecnologia S.A.", "10001005000105", "Genetica e Entretenimento", "contato@ingen.com.br", "11990050001", true, now, now));
            log.info("Preloading " + ingen);
            log.info("Preloading " + contactRepository.save(new Contact(ingen.getId(), "John Hammond", "Fundador e Presidente", "john.hammond@ingen.com.br", "11990050002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(ingen.getId(), "Henry Wu", "Geneticista Chefe", "henry.wu@ingen.com.br", "11990050003", false, true, now, now)));

            Company cyberdyne = companyRepository.save(new Company("Cyberdyne", "Cyberdyne Systems do Brasil Ltda.", "10001006000106", "Robotica e Inteligencia Artificial", "contato@cyberdyne.com.br", "11990060001", true, now, now));
            log.info("Preloading " + cyberdyne);
            log.info("Preloading " + contactRepository.save(new Contact(cyberdyne.getId(), "Miles Dyson", "Diretor de Engenharia", "miles.dyson@cyberdyne.com.br", "11990060002", true, true, now, now)));

            Company acme = companyRepository.save(new Company("Acme", "Acme Corporation Ltda.", "10001007000107", "Varejo e Manufatura", "contato@acme.com.br", "11990070001", true, now, now));
            log.info("Preloading " + acme);
            log.info("Preloading " + contactRepository.save(new Contact(acme.getId(), "Wile E. Coiote", "Gerente de Testes de Produtos", "coiote@acme.com.br", "11990070002", true, true, now, now)));

            Company globex = companyRepository.save(new Company("Globex", "Globex Corporation S.A.", "10001008000108", "Tecnologia e Conglomerado", "contato@globex.com.br", "11990080001", true, now, now));
            log.info("Preloading " + globex);
            log.info("Preloading " + contactRepository.save(new Contact(globex.getId(), "Hank Scorpio", "Diretor Executivo", "hank.scorpio@globex.com.br", "11990080002", true, true, now, now)));

            Company weyland = companyRepository.save(new Company("Weyland-Yutani", "Weyland-Yutani Exploracao Espacial S.A.", "10001014000114", "Aeroespacial e Mineracao", "contato@weylandyutani.com.br", "11990140001", true, now, now));
            log.info("Preloading " + weyland);
            log.info("Preloading " + contactRepository.save(new Contact(weyland.getId(), "Ellen Ripley", "Oficial de Voo", "ellen.ripley@weylandyutani.com.br", "11990140002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(weyland.getId(), "Carter Burke", "Diretor de Operacoes", "carter.burke@weylandyutani.com.br", "11990140003", false, true, now, now)));

            Company aperture = companyRepository.save(new Company("Aperture", "Aperture Science Ltda.", "10001015000115", "Pesquisa Cientifica", "contato@aperture.com.br", "11990150001", true, now, now));
            log.info("Preloading " + aperture);
            log.info("Preloading " + contactRepository.save(new Contact(aperture.getId(), "Cave Johnson", "Fundador e Diretor Executivo", "cave.johnson@aperture.com.br", "11990150002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(aperture.getId(), "GLaDOS", "Gerente de Laboratorio", "glados@aperture.com.br", "11990150003", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(aperture.getId(), "Chell", "Voluntaria de Testes", "chell@aperture.com.br", "11990150004", false, true, now, now)));

            Company vault = companyRepository.save(new Company("Vault-Tec", "Vault-Tec Seguranca S.A.", "10001016000116", "Defesa e Construcao", "contato@vaulttec.com.br", "11990160001", true, now, now));
            log.info("Preloading " + vault);
            log.info("Preloading " + contactRepository.save(new Contact(vault.getId(), "Cooper Howard", "Consultor e Porta-voz", "cooper.howard@vaulttec.com.br", "11990160002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(vault.getId(), "Lucy MacLean", "Representante de Abrigo", "lucy.maclean@vaulttec.com.br", "11990160003", false, true, now, now)));

            Company pollos = companyRepository.save(new Company("Los Pollos Hermanos", "Los Pollos Hermanos Alimentos Ltda.", "10001017000117", "Alimentacao e Restaurante", "contato@lospollos.com.br", "11990170001", true, now, now));
            log.info("Preloading " + pollos);
            log.info("Preloading " + contactRepository.save(new Contact(pollos.getId(), "Gus Fring", "Proprietario e Gerente", "gus.fring@lospollos.com.br", "11990170002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(pollos.getId(), "Mike Ehrmantraut", "Chefe de Seguranca", "mike@lospollos.com.br", "11990170003", false, true, now, now)));

            Company vought = companyRepository.save(new Company("Vought", "Vought International S.A.", "10001018000118", "Entretenimento e Defesa", "contato@vought.com.br", "11990180001", true, now, now));
            log.info("Preloading " + vought);
            log.info("Preloading " + contactRepository.save(new Contact(vought.getId(), "John Homelander", "Diretor de Herois", "homelander@vought.com.br", "11990180002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(vought.getId(), "Annie January", "Heroina e Relacoes Publicas", "starlight@vought.com.br", "11990180003", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(vought.getId(), "Ashley Barrett", "Diretora de Marketing", "ashley.barrett@vought.com.br", "11990180004", false, true, now, now)));

            Company planeta = companyRepository.save(new Company("Planeta Diario", "Planeta Diario Comunicacao S.A.", "10001019000119", "Midia e Jornalismo", "contato@planetadiario.com.br", "11990190001", true, now, now));
            log.info("Preloading " + planeta);
            log.info("Preloading " + contactRepository.save(new Contact(planeta.getId(), "Clark Kent", "Reporter", "clark.kent@planetadiario.com.br", "11990190002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(planeta.getId(), "Lois Lane", "Reporter Senior", "lois.lane@planetadiario.com.br", "11990190003", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(planeta.getId(), "Jimmy Olsen", "Fotografo", "jimmy.olsen@planetadiario.com.br", "11990190004", false, true, now, now)));

            Company weasley = companyRepository.save(new Company("Gemialidades Weasley", "Gemialidades Weasley Comercio Ltda.", "10001022000122", "Varejo e Brinquedos", "contato@weasley.com.br", "11990220001", true, now, now));
            log.info("Preloading " + weasley);
            log.info("Preloading " + contactRepository.save(new Contact(weasley.getId(), "Fred Weasley", "Socio Fundador", "fred.weasley@weasley.com.br", "11990220002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(weasley.getId(), "Jorge Weasley", "Socio Fundador", "jorge.weasley@weasley.com.br", "11990220003", false, true, now, now)));

            Company wonka = companyRepository.save(new Company("Wonka", "Fabrica de Chocolate Wonka S.A.", "10001023000123", "Alimentacao e Doces", "contato@wonka.com.br", "11990230001", true, now, now));
            log.info("Preloading " + wonka);
            log.info("Preloading " + contactRepository.save(new Contact(wonka.getId(), "Willy Wonka", "Fundador e Chocolatier", "willy.wonka@wonka.com.br", "11990230002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(wonka.getId(), "Charlie Bucket", "Herdeiro e Gerente", "charlie.bucket@wonka.com.br", "11990230003", false, true, now, now)));

            Company bnl = companyRepository.save(new Company("Buy n Large", "Buy n Large Varejo S.A.", "10001026000126", "Varejo e Atacado", "contato@buynlarge.com.br", "11990260001", true, now, now));
            log.info("Preloading " + bnl);
            log.info("Preloading " + contactRepository.save(new Contact(bnl.getId(), "Shelby Forthright", "Diretor Executivo", "shelby.forthright@buynlarge.com.br", "11990260002", true, true, now, now)));

            Company monstros = companyRepository.save(new Company("Monstros S.A.", "Monstros S.A. Energia Ltda.", "10001027000127", "Energia e Servicos", "contato@monstrossa.com.br", "11990270001", true, now, now));
            log.info("Preloading " + monstros);
            log.info("Preloading " + contactRepository.save(new Contact(monstros.getId(), "James Sullivan", "Assustador Senior", "sulley@monstrossa.com.br", "11990270002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(monstros.getId(), "Mike Wazowski", "Assistente e Comediante", "mike@monstrossa.com.br", "11990270003", false, true, now, now)));

            Company gusteau = companyRepository.save(new Company("Gusteau's", "Restaurante Gusteau's Ltda.", "10001028000128", "Alimentacao e Restaurante", "contato@gusteaus.com.br", "11990280001", true, now, now));
            log.info("Preloading " + gusteau);
            log.info("Preloading " + contactRepository.save(new Contact(gusteau.getId(), "Alfredo Linguini", "Cozinheiro e Proprietario", "linguini@gusteaus.com.br", "11990280002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(gusteau.getId(), "Colette Tatou", "Chefe de Cozinha", "colette@gusteaus.com.br", "11990280003", false, true, now, now)));

            Company clarim = companyRepository.save(new Company("Clarim Diario", "Clarim Diario Comunicacao Ltda.", "10001030000130", "Midia e Jornalismo", "contato@clarimdiario.com.br", "11990300001", true, now, now));
            log.info("Preloading " + clarim);
            log.info("Preloading " + contactRepository.save(new Contact(clarim.getId(), "J. Jonah Jameson", "Editor-Chefe", "jonah.jameson@clarimdiario.com.br", "11990300002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(clarim.getId(), "Peter Parker", "Fotografo Freelancer", "peter.parker@clarimdiario.com.br", "11990300003", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(clarim.getId(), "Betty Brant", "Secretaria e Reporter", "betty.brant@clarimdiario.com.br", "11990300004", false, true, now, now)));

            Company luthor = companyRepository.save(new Company("LuthorCorp", "LuthorCorp S.A.", "10001031000131", "Tecnologia e Agronegocio", "contato@luthorcorp.com.br", "11990310001", true, now, now));
            log.info("Preloading " + luthor);
            log.info("Preloading " + contactRepository.save(new Contact(luthor.getId(), "Lex Luthor", "Diretor Executivo", "lex.luthor@luthorcorp.com.br", "11990310002", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(luthor.getId(), "Lionel Luthor", "Presidente do Conselho", "lionel.luthor@luthorcorp.com.br", "11990310003", false, true, now, now)));

            Company star = companyRepository.save(new Company("S.T.A.R. Labs", "Laboratorios S.T.A.R. Pesquisa Ltda.", "10001032000132", "Pesquisa Cientifica e Fisica", "contato@starlabs.com.br", "11990320001", true, now, now));
            log.info("Preloading " + star);
            log.info("Preloading " + contactRepository.save(new Contact(star.getId(), "Barry Allen", "Perito Forense", "barry.allen@starlabs.com.br", "11990320002", false, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(star.getId(), "Cisco Ramon", "Engenheiro", "cisco.ramon@starlabs.com.br", "11990320003", true, true, now, now)));
            log.info("Preloading " + contactRepository.save(new Contact(star.getId(), "Caitlin Snow", "Bioengenheira", "caitlin.snow@starlabs.com.br", "11990320004", false, true, now, now)));

            PaymentMethod paymentMethod1 = paymentMethodRepository.save(new PaymentMethod(stark.getId(), "PIX", "Banco Itau", "Conta Principal", "Ag 0001 Cc 12345-6", true, true));
            log.info("Preloading " + paymentMethod1);

            PaymentMethod paymentMethod2 = paymentMethodRepository.save(new PaymentMethod(wayne.getId(), "TRANSFERENCIA", "Banco Gotham", "Conta Operacional", "Ag 0010 Cc 98765-4", true, true));
            log.info("Preloading " + paymentMethod2);

            PaymentMethod paymentMethod3 = paymentMethodRepository.save(new PaymentMethod(umbrella.getId(), "BOLETO", "Banco Bradesco", "Cobranca Pesquisa", "Carteira 09", true, true));
            log.info("Preloading " + paymentMethod3);

            PaymentMethod paymentMethod4 = paymentMethodRepository.save(new PaymentMethod(vought.getId(), "CARTAO", "Operadora Vought", "Cartao Corporativo", "Final 4321", true, true));
            log.info("Preloading " + paymentMethod4);

            PaymentMethod paymentMethod5 = paymentMethodRepository.save(new PaymentMethod(planeta.getId(), "BOLETO", "Banco Itau", "Assinaturas", "Carteira 18", true, true));
            log.info("Preloading " + paymentMethod5);

            PaymentMethod paymentMethod6 = paymentMethodRepository.save(new PaymentMethod(star.getId(), "TRANSFERENCIA", "Banco Central City", "Conta Pesquisa", "Ag 0020 Cc 55555-1", true, true));
            log.info("Preloading " + paymentMethod6);

            Contract contract1 = contractRepository.save(new Contract(stark.getId(), "Fornecimento 2026", "Fornecimento anual", new BigDecimal("150000.00"), "BRL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ATIVO", LocalDateTime.now(), LocalDateTime.now()));
            log.info("Preloading " + contract1);
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract1.getId(), "Parcela 01/12", new BigDecimal("12500.00"), LocalDate.of(2026, 1, 10), now, "PAGO", paymentMethod1.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract1.getId(), "Parcela 02/12", new BigDecimal("12500.00"), LocalDate.of(2026, 11, 10), null, "PENDENTE", paymentMethod1.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract1.getId(), "Parcela 03/12", new BigDecimal("12500.00"), LocalDate.of(2026, 2, 10), null, "ATRASADO", paymentMethod1.getId())));

            Contract contract2 = contractRepository.save(new Contract(wayne.getId(), "Seguranca Gotham 2026", "Monitoramento e blindados", new BigDecimal("240000.00"), "BRL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ATIVO", LocalDateTime.now(), LocalDateTime.now()));
            log.info("Preloading " + contract2);
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract2.getId(), "Parcela 01/06", new BigDecimal("40000.00"), LocalDate.of(2026, 1, 15), now, "PAGO", paymentMethod2.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract2.getId(), "Parcela 02/06", new BigDecimal("40000.00"), LocalDate.of(2026, 11, 15), null, "PENDENTE", paymentMethod2.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract2.getId(), "Parcela 03/06", new BigDecimal("40000.00"), LocalDate.of(2026, 3, 15), null, "ATRASADO", paymentMethod2.getId())));

            Contract contract3 = contractRepository.save(new Contract(umbrella.getId(), "Pesquisa Vacinal 2026", "Desenvolvimento e testes", new BigDecimal("500000.00"), "BRL", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), "ATIVO", LocalDateTime.now(), LocalDateTime.now()));
            log.info("Preloading " + contract3);
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract3.getId(), "Etapa 01/02", new BigDecimal("250000.00"), LocalDate.of(2026, 6, 30), now, "PAGO", paymentMethod3.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract3.getId(), "Etapa 02/02", new BigDecimal("250000.00"), LocalDate.of(2026, 12, 15), null, "PENDENTE", paymentMethod3.getId())));

            Contract contract4 = contractRepository.save(new Contract(vought.getId(), "Licenciamento Herois 2026", "Imagem e eventos", new BigDecimal("1000000.00"), "BRL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ATIVO", LocalDateTime.now(), LocalDateTime.now()));
            log.info("Preloading " + contract4);
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract4.getId(), "Parcela 01/03", new BigDecimal("333333.33"), LocalDate.of(2026, 4, 10), now, "PAGO", paymentMethod4.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract4.getId(), "Parcela 02/03", new BigDecimal("333333.33"), LocalDate.of(2026, 8, 10), null, "PENDENTE", paymentMethod4.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract4.getId(), "Parcela 03/03", new BigDecimal("333333.34"), LocalDate.of(2026, 12, 10), null, "PENDENTE", paymentMethod4.getId())));

            Contract contract5 = contractRepository.save(new Contract(planeta.getId(), "Publicidade 2026", "Anuncios impressos e digitais", new BigDecimal("80000.00"), "BRL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), "ATIVO", LocalDateTime.now(), LocalDateTime.now()));
            log.info("Preloading " + contract5);
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract5.getId(), "Parcela 01/02", new BigDecimal("40000.00"), LocalDate.of(2026, 2, 20), now, "PAGO", paymentMethod5.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract5.getId(), "Parcela 02/02", new BigDecimal("40000.00"), LocalDate.of(2026, 5, 20), null, "PENDENTE", paymentMethod5.getId())));

            Contract contract6 = contractRepository.save(new Contract(star.getId(), "Pesquisa Particulas 2026", "Acelerador e sensores", new BigDecimal("300000.00"), "BRL", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31), "ATIVO", LocalDateTime.now(), LocalDateTime.now()));
            log.info("Preloading " + contract6);
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract6.getId(), "Etapa 01/02", new BigDecimal("150000.00"), LocalDate.of(2026, 5, 30), now, "PAGO", paymentMethod6.getId())));
            log.info("Preloading " + invoiceRepository.save(new Invoice(contract6.getId(), "Etapa 02/02", new BigDecimal("150000.00"), LocalDate.of(2026, 11, 30), null, "PENDENTE", paymentMethod6.getId())));
        };
    }
}
