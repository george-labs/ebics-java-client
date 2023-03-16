/*
 * Copyright (c) 1990-2012 kopiLeft Development SARL, Bizerte, Tunisia
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id$
 */

package org.kopi.ebics.xml;

import java.util.Calendar;

import javax.crypto.spec.SecretKeySpec;

import org.kopi.ebics.exception.EbicsException;
import org.kopi.ebics.schema.h004.DataEncryptionInfoType.EncryptionPubKeyDigest;
import org.kopi.ebics.schema.h004.DataTransferRequestType;
import org.kopi.ebics.schema.h004.DataTransferRequestType.DataEncryptionInfo;
import org.kopi.ebics.schema.h004.DataTransferRequestType.SignatureData;
import org.kopi.ebics.schema.h004.EbicsRequestDocument.EbicsRequest;
import org.kopi.ebics.schema.h004.EbicsRequestDocument.EbicsRequest.Body;
import org.kopi.ebics.schema.h004.EbicsRequestDocument.EbicsRequest.Header;
import org.kopi.ebics.schema.h004.MutableHeaderType;
import org.kopi.ebics.schema.h004.OrderAttributeType;
import org.kopi.ebics.schema.h004.StandardOrderParamsType;
import org.kopi.ebics.schema.h004.StaticHeaderOrderDetailsType;
import org.kopi.ebics.schema.h004.StaticHeaderOrderDetailsType.OrderType;
import org.kopi.ebics.schema.h004.StaticHeaderType;
import org.kopi.ebics.schema.h004.StaticHeaderType.BankPubKeyDigests;
import org.kopi.ebics.schema.h004.StaticHeaderType.BankPubKeyDigests.Authentication;
import org.kopi.ebics.schema.h004.StaticHeaderType.BankPubKeyDigests.Encryption;
import org.kopi.ebics.schema.h004.StaticHeaderType.Product;
import org.kopi.ebics.session.EbicsSession;
import org.kopi.ebics.utils.Utils;


/**
 * The <code>SPRRequestElement</code> is the request element
 * for revoking a subscriber
 *
 * @author Hachani
 *
 */
public class SPRRequestElement extends InitializationRequestElement {

  /**
   * Constructs a new SPR request element.
   * @param session the current ebic session.
   */
  public SPRRequestElement(EbicsSession session) throws EbicsException {
    super(session, org.kopi.ebics.session.OrderType.SPR, "SPRRequest.xml");
    keySpec = new SecretKeySpec(nonce, "EAS");
  }

  @Override
  public void buildInitialization() throws EbicsException {
    EbicsRequest			request;
    Header 				header;
    Body				body;
    MutableHeaderType 			mutable;
    StaticHeaderType 			xstatic;
    Product 				product;
    BankPubKeyDigests 			bankPubKeyDigests;
    Authentication 			authentication;
    Encryption 				encryption;
    DataTransferRequestType 		dataTransfer;
    DataEncryptionInfo 			dataEncryptionInfo;
    SignatureData 			signatureData;
    EncryptionPubKeyDigest 		encryptionPubKeyDigest;
    StaticHeaderOrderDetailsType 	orderDetails;
    OrderType 				orderType;
    StandardOrderParamsType		standardOrderParamsType;
    UserSignature			userSignature;

    userSignature = new UserSignature(session.getUser(),
				      generateName("SIG"),
	                              session.getConfiguration().getSignatureVersion(),
	                              " ".getBytes());
    userSignature.build();
    userSignature.validate();

    mutable = EbicsXmlFactory.createMutableHeaderType("Initialisation", null);
    product = EbicsXmlFactory.createProduct(session.getProduct().getLanguage(), session.getProduct().getName());
    authentication = EbicsXmlFactory.createAuthentication(session.getConfiguration().getAuthenticationVersion(),
	                                                  "http://www.w3.org/2001/04/xmlenc#sha256",
	                                                  decodeHex(session.getUser().getPartner().getBank().getX002Digest()));
    encryption = EbicsXmlFactory.createEncryption(session.getConfiguration().getEncryptionVersion(),
	                                          "http://www.w3.org/2001/04/xmlenc#sha256",
	                                          decodeHex(session.getUser().getPartner().getBank().getE002Digest()));
    bankPubKeyDigests = EbicsXmlFactory.createBankPubKeyDigests(authentication, encryption);
    orderType = EbicsXmlFactory.createOrderType(type.getCode());
    standardOrderParamsType = EbicsXmlFactory.createStandardOrderParamsType();
    orderDetails = EbicsXmlFactory.createStaticHeaderOrderDetailsType(session.getUser().getPartner().nextOrderId(),
	                                                              OrderAttributeType.UZHNN,
	                                                              orderType,
	                                                              standardOrderParamsType);
    xstatic = EbicsXmlFactory.createStaticHeaderType(session.getBankID(),
	                                             nonce,
	                                             0,
	                                             session.getUser().getPartner().getPartnerId(),
	                                             product,
	                                             session.getUser().getSecurityMedium(),
	                                             session.getUser().getUserId(),
	                                             Calendar.getInstance(),
	                                             orderDetails,
	                                             bankPubKeyDigests);
    header = EbicsXmlFactory.createEbicsRequestHeader(true, mutable, xstatic);
    encryptionPubKeyDigest = EbicsXmlFactory.createEncryptionPubKeyDigest(session.getConfiguration().getEncryptionVersion(),
								          "http://www.w3.org/2001/04/xmlenc#sha256",
								          decodeHex(session.getUser().getPartner().getBank().getE002Digest()));
    signatureData = EbicsXmlFactory.createSignatureData(true, Utils.encrypt(Utils.zip(userSignature.prettyPrint()), keySpec));
    dataEncryptionInfo = EbicsXmlFactory.createDataEncryptionInfo(true,
	                                                          encryptionPubKeyDigest,
	                                                          generateTransactionKey());
    dataTransfer = EbicsXmlFactory.createDataTransferRequestType(dataEncryptionInfo, signatureData);
    body = EbicsXmlFactory.createEbicsRequestBody(dataTransfer);
    request = EbicsXmlFactory.createEbicsRequest(session.getConfiguration().getRevision(),
	                                         session.getConfiguration().getVersion(),
	                                         header,
	                                         body);
    document = EbicsXmlFactory.createEbicsRequestDocument(request);
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private SecretKeySpec			keySpec;
  private static final long 		serialVersionUID = -6742241777786111337L;
}
