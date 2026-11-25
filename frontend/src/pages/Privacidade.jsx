import { useLoja } from "../lib/store";

/** Política de Privacidade (LGPD), exigida pela Meta para publicar o robô do WhatsApp. */
export default function Privacidade() {
  const { config } = useLoja();
  const email = config.email || "presentya2026@gmail.com";
  return (
    <section className="sec">
      <div className="wrap legal-pg">
        <span className="eyebrow">PresentYá</span>
        <h1>Política de Privacidade</h1>
        <p><small>Última atualização: 25 de novembro de 2026</small></p>

        <p>Esta política explica quais dados pessoais a PresentYá (São Luís – MA) coleta na loja virtual, no app,
          no WhatsApp e no Telegram, para que usamos e como você pode pedir a exclusão, conforme a Lei Geral de
          Proteção de Dados (Lei 13.709/2018 – LGPD).</p>

        <h2>Quais dados coletamos</h2>
        <ul>
          <li><b>Cadastro:</b> nome, e-mail, telefone e senha (guardada de forma criptografada; nem a loja consegue vê-la).</li>
          <li><b>Pedidos:</b> produtos, valores, endereço de entrega, nome de quem recebe, mensagem do cartão e forma de pagamento.
            Os dados do cartão são digitados direto no Mercado Pago: <b>a PresentYá não recebe nem guarda dados de cartão</b>.</li>
          <li><b>Ofertas no WhatsApp e no Telegram:</b> quando você manda mensagem para o robô de ofertas, guardamos seu número
            (ou identificação do Telegram), seu nome no aplicativo e a data da última mensagem.</li>
          <li><b>Lista de interessados:</b> nome, WhatsApp e e-mail que você informar para receber novidades.</li>
          <li><b>Notificações do app:</b> se você ativar, guardamos o código que o navegador gera para entregar os avisos
            (não identifica você pelo nome).</li>
          <li><b>Localização:</b> o aviso “você está perto da loja” usa a localização <b>somente no seu celular</b>, e só se você
            permitir. Ela <b>não é enviada nem guardada</b> pela PresentYá.</li>
        </ul>

        <h2>Para que usamos</h2>
        <ul>
          <li>Processar e entregar pedidos, emitir comprovantes e prestar atendimento.</li>
          <li>Enviar ofertas, promoções e novidades a quem pediu para receber (WhatsApp, Telegram, notificações ou e-mail).</li>
          <li>Administrar o Clube PresentYá (pontos de fidelidade).</li>
          <li>Cumprir obrigações legais e fiscais.</li>
        </ul>
        <p>Não vendemos nem alugamos seus dados.</p>

        <h2>Com quem compartilhamos</h2>
        <p>Somente com os serviços necessários para a loja funcionar: Mercado Pago (pagamentos), Meta/WhatsApp e Telegram
          (mensagens), serviços de hospedagem e banco de dados da loja, e transportadoras ou Correios para a entrega.</p>

        <h2>Por quanto tempo guardamos</h2>
        <p>Dados de cadastro e de contato ficam guardados enquanto você usar nossos serviços ou até pedir a exclusão.
          Dados de pedidos podem ser mantidos pelo prazo exigido pela legislação fiscal.</p>

        <h2 id="exclusao">Seus direitos e como excluir seus dados</h2>
        <p>Você pode, a qualquer momento, pedir acesso, correção ou exclusão dos seus dados:</p>
        <ul>
          <li><b>Parar de receber ofertas:</b> mande <b>PARAR</b> para o robô no WhatsApp ou no Telegram; seu contato sai da lista na hora.</li>
          <li><b>Notificações:</b> desative nas configurações do navegador ou do celular.</li>
          <li><b>Excluir todos os seus dados:</b> envie um e-mail para <a href={`mailto:${email}?subject=Exclus%C3%A3o%20de%20dados`}>{email}</a> com
            o assunto “Exclusão de dados”, informando o e-mail ou telefone usado. Respondemos em até 15 dias.</li>
        </ul>

        <h2>Contato</h2>
        <p>PresentYá · São Luís – MA · <a href={`mailto:${email}`}>{email}</a>
          {config.whatsapp_exibicao ? <> · WhatsApp {config.whatsapp_exibicao}</> : null}</p>
      </div>
    </section>
  );
}
