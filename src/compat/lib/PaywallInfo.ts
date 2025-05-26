import type {
  PaywallInfo as MainPaywallInfo,
  Experiment as MainExperiment,
  Product as MainProduct,
  FeatureGatingBehavior as MainFeatureGatingBehavior,
  PaywallCloseReason as MainPaywallCloseReason,
  LocalNotification as MainLocalNotification,
  ComputedPropertyRequest as MainComputedPropertyRequest,
  Survey as MainSurvey,
} from '../../SuperwallExpoModule.types';

// Keep existing local class imports for now, as they have fromJson methods
import { ComputedPropertyRequest } from "./ComputedPropertyRequest";
import { Experiment } from "./Experiment";
import { featureGatingBehaviorFromJson } from "./FeatureGatingBehavior";
import { LocalNotification } from "./LocalNotification";
import { PaywallCloseReason } from "./PaywallCloseReason";
import { Product } from "./Product";
import { Survey } from "./Survey";

export class PaywallInfo implements MainPaywallInfo {
  identifier: string;
  name: string;
  url: string;
  experiment?: MainExperiment; // Use MainExperiment type
  products: MainProduct[];     // Use MainProduct type
  productIds: string[];
  presentedByEventWithName?: string;
  presentedByEventWithId?: string;
  presentedByEventAt?: number; // Changed to number | undefined
  presentedBy: string;
  presentationSourceType?: string;
  responseLoadStartTime?: number; // Changed to number | undefined
  responseLoadCompleteTime?: number; // Changed to number | undefined
  responseLoadFailTime?: number; // Changed to number | undefined
  responseLoadDuration?: number;
  webViewLoadStartTime?: number; // Changed to number | undefined
  webViewLoadCompleteTime?: number; // Changed to number | undefined
  webViewLoadFailTime?: number; // Changed to number | undefined
  webViewLoadDuration?: number;
  productsLoadStartTime?: number; // Changed to number | undefined
  productsLoadCompleteTime?: number; // Changed to number | undefined
  productsLoadFailTime?: number; // Changed to number | undefined
  productsLoadDuration?: number;
  paywalljsVersion?: string;
  isFreeTrialAvailable: boolean;
  featureGatingBehavior: MainFeatureGatingBehavior; // Use MainFeatureGatingBehavior type
  closeReason: MainPaywallCloseReason;             // Use MainPaywallCloseReason type
  localNotifications: MainLocalNotification[];     // Use MainLocalNotification type
  computedPropertyRequests: MainComputedPropertyRequest[]; // Use MainComputedPropertyRequest type
  surveys: MainSurvey[];                           // Use MainSurvey type

  constructor({
    identifier,
    name,
    url,
    experiment,
    products,
    productIds,
    presentedByEventWithName,
    presentedByEventWithId,
    presentedByEventAt,
    presentedBy,
    presentationSourceType,
    responseLoadStartTime,
    responseLoadCompleteTime,
    responseLoadFailTime,
    responseLoadDuration,
    webViewLoadStartTime,
    webViewLoadCompleteTime,
    webViewLoadFailTime,
    webViewLoadDuration,
    productsLoadStartTime,
    productsLoadCompleteTime,
    productsLoadFailTime,
    productsLoadDuration,
    paywalljsVersion,
    isFreeTrialAvailable,
    featureGatingBehavior,
    closeReason,
    localNotifications,
    computedPropertyRequests,
    surveys,
  }: MainPaywallInfo) { // Constructor now accepts MainPaywallInfo
    this.identifier = identifier;
    this.name = name;
    this.url = url;
    this.experiment = experiment;
    this.products = products;
    this.productIds = productIds;
    this.presentedByEventWithName = presentedByEventWithName;
    this.presentedByEventWithId = presentedByEventWithId;
    this.presentedByEventAt = presentedByEventAt;
    this.presentedBy = presentedBy;
    this.presentationSourceType = presentationSourceType;
    this.responseLoadStartTime = responseLoadStartTime;
    this.responseLoadCompleteTime = responseLoadCompleteTime;
    this.responseLoadFailTime = responseLoadFailTime;
    this.responseLoadDuration = responseLoadDuration;
    this.webViewLoadStartTime = webViewLoadStartTime;
    this.webViewLoadCompleteTime = webViewLoadCompleteTime;
    this.webViewLoadFailTime = webViewLoadFailTime;
    this.webViewLoadDuration = webViewLoadDuration;
    this.productsLoadStartTime = productsLoadStartTime;
    this.productsLoadCompleteTime = productsLoadCompleteTime;
    this.productsLoadFailTime = productsLoadFailTime;
    this.productsLoadDuration = productsLoadDuration;
    this.paywalljsVersion = paywalljsVersion;
    this.isFreeTrialAvailable = isFreeTrialAvailable;
    this.featureGatingBehavior = featureGatingBehavior;
    this.closeReason = closeReason;
    this.localNotifications = localNotifications;
    this.computedPropertyRequests = computedPropertyRequests;
    this.surveys = surveys;
  }

  static fromJson(json: any): PaywallInfo {
    // Helper to parse string to number, returns undefined if input is null/undefined or NaN
    const parseOptionalFloat = (val: any): number | undefined => {
      if (val === null || val === undefined) return undefined;
      const num = parseFloat(val);
      return isNaN(num) ? undefined : num;
    };

    // For nested objects that are classes with their own fromJson, we still use them.
    // For properties that should be the main types, we directly assign or parse.
    const experiment = json.experiment ? Experiment.fromJson(json.experiment) : undefined;
    const products = json.products ? json.products.map((p: any) => Product.fromJson(p)) : [];
    const localNotifications = json.localNotifications ? json.localNotifications.map((n: any) => LocalNotification.fromJson(n)) : [];
    const computedPropertyRequests = json.computedPropertyRequests ? json.computedPropertyRequests.map((r: any) => ComputedPropertyRequest.fromJson(r)) : [];
    const surveys = json.surveys ? json.surveys.map((s: any) => Survey.fromJson(s)) : [];

    return new PaywallInfo({
      identifier: json.identifier,
      name: json.name,
      url: json.url,
      experiment: experiment as MainExperiment | undefined, // Cast to MainExperiment for assignment
      products: products as MainProduct[],                 // Cast to MainProduct array
      productIds: json.productIds,
      presentedByEventWithName: json.presentedByEventWithName,
      presentedByEventWithId: json.presentedByEventWithId,
      presentedByEventAt: parseOptionalFloat(json.presentedByEventAt),
      presentedBy: json.presentedBy,
      presentationSourceType: json.presentationSourceType,
      responseLoadStartTime: parseOptionalFloat(json.responseLoadStartTime),
      responseLoadCompleteTime: parseOptionalFloat(json.responseLoadCompleteTime),
      responseLoadFailTime: parseOptionalFloat(json.responseLoadFailTime),
      responseLoadDuration: parseOptionalFloat(json.responseLoadDuration),
      webViewLoadStartTime: parseOptionalFloat(json.webViewLoadStartTime),
      webViewLoadCompleteTime: parseOptionalFloat(json.webViewLoadCompleteTime),
      webViewLoadFailTime: parseOptionalFloat(json.webViewLoadFailTime),
      webViewLoadDuration: parseOptionalFloat(json.webViewLoadDuration),
      productsLoadStartTime: parseOptionalFloat(json.productsLoadStartTime),
      productsLoadCompleteTime: parseOptionalFloat(json.productsLoadCompleteTime),
      productsLoadFailTime: parseOptionalFloat(json.productsLoadFailTime),
      productsLoadDuration: parseOptionalFloat(json.productsLoadDuration),
      paywalljsVersion: json.paywalljsVersion,
      isFreeTrialAvailable: json.isFreeTrialAvailable,
      featureGatingBehavior: featureGatingBehaviorFromJson(json.featureGatingBehavior) as MainFeatureGatingBehavior,
      closeReason: PaywallCloseReason.fromJson(json.closeReason) as MainPaywallCloseReason,
      localNotifications: localNotifications as MainLocalNotification[], // Cast
      computedPropertyRequests: computedPropertyRequests as MainComputedPropertyRequest[], // Cast
      surveys: surveys as MainSurvey[], // Cast
    });
  }
}
