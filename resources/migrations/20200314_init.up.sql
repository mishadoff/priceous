-- Table related to the scrapping jobs
create table jobs (
    id text primary key,
    type text not null,
    status text not null,
    data jsonb,
    started_ts timestamptz not null,
    finished_ts timestamptz
);

-- Make jobs fast
create index jobs_type_idx on jobs (type);
create index jobs_status_idx on jobs (status);
create index jobs_started_ts_idx on jobs (started_ts);
create index jobs_finished_ts_idx on jobs (finished_ts);

-- Contains information about all providers of data
create table providers (
    id text primary key, -- code name of provider
    title text not null, -- visible name of provider
    base_url text not null, -- link to the site
    icon text, -- link to the icon
    icon_w text,
    icon_h text,
    is_trusted boolean not null -- is it trusted store
);

-- Historical product
create table products (
    id text primary key, -- synthetic id identifier of the row
    provider_id text references providers(id), -- provider of the data, e.g. goodwine, winetime

    -- product attributes
    name text not null, -- name of the product
    description text, -- description of the product, can contain tasting notes as well
    category text, -- category which this product belong to (e.g white wine, whisky)
    link text not null, -- link where we get information about this product
    image_link text, -- link to the image
    country text, -- country, producer of the product
    wine_sugar text, -- applicable to wines, amount of sugar gram/liter
    wine_grape text, -- applicable to wines, grape used for production
    year_of_origin text, -- year of origin
    age text, -- age of product, e.g Laphroaig 10 years
    producer text, -- company which produce the product
    article_code text, -- code used to identify product on site
    abv numeric, -- alcohol by volume in %
    volume numeric, -- volume of bottle in ml
    price numeric, -- price of product in uah
    is_available boolean not null, -- availability of product
    is_new boolean not null, -- recent products
    is_sale boolean not null, -- product on sale
    sale_description text, -- applicable if product on sale
    is_excise boolean not null, -- does tax paid for this product

    -- system information
    created_at timestamptz not null, -- time when product appeared
    job_id text references jobs(id) -- product created by the following job
);

create index products_link_idx on products(link);
create index products_created_at_idx on products(created_at);